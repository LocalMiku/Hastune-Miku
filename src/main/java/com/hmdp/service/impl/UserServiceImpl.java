package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
//说一下嗷，这个ServiceImpl是 mybaitsPlus给我们提供的轮子，可以实现单表查询
//如果我也想有这个功能，1导入包2   下面这个继承 mapperdao层也交给mybaits
// 如果想自己写sql，就要写usermapper的xml文件，在里面写sql。然后怎么用呢
//存疑忘记了，就是写自己mapper。xml文件，然后同时继承 mp的mapper和自己的mapper
    //还有具体细节，比如传参，这个mp和mybaits都要复习
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    //按 byName自动注入罢了。@Resource有两个属性是比较重要的，
    // 分是name和type，Spring将@Resource注解的name属性解析为bean的名字，
    // 而type属性则解析为bean的类型。
    // 所以如果使用name属性，则使用byName的自动注入策略，
    // 而使用type属性时则使用byType自动注入策略。
    // 如果既不指定name也不指定type属性，这时将通过反射机制使用byName自动注入策略。
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //redis优化说明，业务流程 发送验证码，服务端存到redis里

        //1后端校验手机格式，使用正则工具包,如果不对，直接把Result返回过去，这个是我们前后
        //交互的协议，里面放了很多东西
        if(RegexUtils.isPhoneInvalid(phone))
        {
            return Result.fail("o(╥﹏╥)o格式不对（づ￣3￣）づ╭❤～");
        }
        //2如果符合，就为这个手机号生成验证码
        String Code= RandomUtil.randomNumbers(6);
        //3为了用于校验，我要把Code储存进session,问题，session是什么？是一个域，而这个session是专属的吗？
        //问题就在于这里没弄太明白
        //session的出现，应用层http协议打的补丁，为了维持客户端状态而出现的
        //大概是一个连接一个session，cookie里的sessionid 来选择session
        //然后一个用户一个session，他的东西全在里面存着
        //session.setAttribute("Code",Code);
        //将数据存入redis--与之相关的登陆业务，还有登录状态校验业务
        stringRedisTemplate.opsForValue().set("logincode:"+phone,Code);
        //4我们需要把验证码发送给客户，调用发送模块

        log.debug("（づ￣3￣）づ╭❤～miku（づ￣3￣）づ╭❤～爱你:"+Code);
        //返回，前端申请的校验完成
        return Result.ok();

        //补充说明
        //　a)web.xml中
        //
        //　　<session-config> <session-timeout>30</session-timeout> </session-config>
        //
        //　　b)在程序中手动设置
        //
        //　　session.setMaxInactiveInterval(30 * 60);//设置单位为秒，设置为-1永不过期
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //这里完成的是登录注册的逻辑，  想象那个页面，用户先点 发送验证码，成功了之后就存到session里了
        //1 根据提交过来的表单 loginForm校验
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone()))
        {
            return Result.fail("o(╥﹏╥)o格式不对（づ￣3￣）づ╭❤～");
        }
        //2 手机号格式正确，校验验证码，session是专属这个用户请求对话的session
       // String Code= (String) session.getAttribute("Code");
        //session.removeAttribute("Code");
        String Code=stringRedisTemplate.opsForValue().get("logincode:"+loginForm.getPhone());
        if(Code==null||!Code.equals(loginForm.getCode()))
        {
            return Result.fail("（づ￣3￣）づ╭❤～验证码错啦");
        }
        //3验证码正确，那么就要去查询用户是否存在
        User user=query().eq("phone",loginForm.getPhone()).one();//MP内容，还不会
        if(user==null)
        {
            //创建此phone的用户在数据库
            //然后再存入session，session是由cookie里的sessionid来找到的，专属于一个用户
            //这个cookie也就是用户的浏览器专属的
            user=Create(loginForm.getPhone());

        }

        //将user存入session，用以维持客户端状态
        //生成uui作为登录令牌
        //将user转为map，以便放入redis
        String token = UUID.randomUUID().toString(true);

        UserDTO userDTO= BeanUtil.copyProperties(user,UserDTO.class);
        Map<String,Object> userMap=BeanUtil.beanToMap(userDTO,new HashMap<>()
        , CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //储存 哈希值 登录令牌-用户 哈希键值对
        String tokenKey="loginkey"+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
        stringRedisTemplate.expire(tokenKey,2, TimeUnit.MINUTES);
        return Result.ok(token);



    }
    private User Create(String phone){
        User user=new User();
        user.setPhone(phone);
        user.setNickName(RandomUtil.randomString(5)+"miku（づ￣3￣）づ╭❤～");
        save(user);//MP的保存 就在那个imp里
        return user;
    }

}
