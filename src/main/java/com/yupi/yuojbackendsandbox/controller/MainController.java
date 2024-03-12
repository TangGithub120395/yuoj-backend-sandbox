package com.yupi.yuojbackendsandbox.controller;

import com.yupi.yuojbackendsandbox.JavaDockerCodeSandBox;
import com.yupi.yuojbackendsandbox.JavaNativeCodeSandBox;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeRequest;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/4 20:16
 */
@RestController
public class MainController {

    //定义健全请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "secretKey";
    @Resource
    private JavaDockerCodeSandBox javaDockerCodeSandBox;

    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                           HttpServletRequest request,
                                           HttpServletResponse response){

        String header = request.getHeader(AUTH_REQUEST_HEADER);
        if(!header.equals(AUTH_REQUEST_SECRET)){
            response.setStatus(403);;
            return null;
        }
        System.out.println(executeCodeRequest);
        if(executeCodeRequest == null){
            throw new RuntimeException("请求参数无效");
        }
        return javaDockerCodeSandBox.executeCode(executeCodeRequest);
    }
}
