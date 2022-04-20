package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.apache.catalina.Session;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//登录校验的入口，通过mvc拦截器实现
//用以维持用户是否登录过，本来在对应的接口里写，但是都需要校验，所以直接写这里
//对所有请求，之前都进行一次校验
public class MyInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1维持客户端状态的session里面是否存了用户，这个session单独对一个用户的浏览器生效
        HttpSession session=request.getSession();
        Object user=session.getAttribute("user");
        //2 这个session里维持了客户端状态，里面是否有user呢？就是登陆过没
        if(user==null)
        {
            response.setStatus(401);
            return false;
        }
        //3如果存在，就把这个用户保存在 服务器应用的 专属于它的线程里
        UserHolder.saveUser((UserDTO) user);
        return true;//放行了



    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
       UserHolder.removeUser();
    }
}
