package com.yupi.yuojbackendsandbox;

import com.yupi.yuojbackendsandbox.model.ExecuteCodeRequest;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeResponse;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/1 16:11
 */
public interface CodeSandBox {
    /**
     * 执行用户提交的代码
     * @param executeCodeRequest 需要给代码沙箱传递的数据
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
