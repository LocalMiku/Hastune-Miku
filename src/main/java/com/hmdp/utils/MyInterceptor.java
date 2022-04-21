package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.apache.catalina.Session;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

//登录校验的入口，通过mvc拦截器实现
//用以维持用户是否登录过，本来在对应的接口里写，但是都需要校验，所以直接写这里
//对所有请求，之前都进行一次校验
public class MyInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;
    public MyInterceptor(StringRedisTemplate stringRedisTemplate)
    {
        this.stringRedisTemplate=stringRedisTemplate;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /*//1维持客户端状态的session里面是否存了用户，这个session单独对一个用户的浏览器生效
        HttpSession session=request.getSession();
        User user=(User)session.getAttribute("user");
        //2 这个session里维持了客户端状态，里面是否有user呢？就是登陆过没
        if(user==null)
        {
            response.setStatus(401);
            return false;
        }
        //3如果存在，就把这个用户保存在 服务器应用的 专属于它的线程里
        UserDTO userDTO=new UserDTO();
        userDTO.setIcon(user.getIcon());
        userDTO.setId(user.getId());
        userDTO.setNickName(user.getNickName());
        UserHolder.saveUser(userDTO);
        return true;//放行了*/

        // 1.获取请求头中的token 前端定义个token头值
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }
        // 2.基于TOKEN获取redis中的用户
        String key  = "loginkey" + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        // 3.判断用户是否存在
        if (userMap.isEmpty()) {
            return true;
        }
        // 5.将查询到的hash数据转为UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 6.存在，保存用户信息到 ThreadLocal
        UserHolder.saveUser(userDTO);
        // 7.刷新token有效期
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 8.放行
        return true;



    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
       UserHolder.removeUser();
    }
}
