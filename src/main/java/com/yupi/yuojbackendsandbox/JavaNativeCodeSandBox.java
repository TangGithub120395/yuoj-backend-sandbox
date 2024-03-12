package com.yupi.yuojbackendsandbox;

import com.yupi.yuojbackendsandbox.model.ExecuteCodeRequest;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/4 19:51
 */
@Component
public class JavaNativeCodeSandBox extends JavaCodeSandBoxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
