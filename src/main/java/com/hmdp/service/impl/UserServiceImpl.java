package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Override
    public Result sendCode(String phone, HttpSession session) {

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
        session.setAttribute("Code",Code);
        //4我们需要把验证码发送给客户

        log.debug("（づ￣3￣）づ╭❤～miku（づ￣3￣）づ╭❤～爱你:"+Code);



        //返回，前端申请的校验完成
        return Result.ok();
    }

}
