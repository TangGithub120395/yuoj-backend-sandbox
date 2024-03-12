package com.yupi.yuojbackendsandbox.utils;

import com.yupi.yuojbackendsandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/2 19:07
 */
public class ProcessUtils {
    public static ExecuteMessage runProcessAndGetMessage(Process process, String operationName) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Integer exitValue;
        try {
            exitValue = process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        BufferedReader inputBufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
        StringBuilder inputCompileOutputStringBuilder = new StringBuilder();
        if (exitValue == 0) {
            System.out.println(operationName + "成功");
        } else {
            System.out.println(operationName + "失败");
        }
        stopWatch.stop();
        // 逐行读取
        //获取错误信息
        String errorCompileOutputLine;
        try {
            while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                errorCompileOutputStringBuilder.append(errorCompileOutputLine).append("\n");
            }
            //获取输出信息
            String inputCompileOutputLine;
            while ((inputCompileOutputLine = inputBufferedReader.readLine()) != null) {
                inputCompileOutputStringBuilder.append(inputCompileOutputLine).append("\n");
            }
            if (errorCompileOutputStringBuilder.length() != 0) {
                errorCompileOutputStringBuilder.deleteCharAt(errorCompileOutputStringBuilder.length() - 1);
            }
            if (inputCompileOutputStringBuilder.length() != 0) {
                inputCompileOutputStringBuilder.deleteCharAt(inputCompileOutputStringBuilder.length() - 1);
            }
//            inputCompileOutputStringBuilder.deleteCharAt(inputCompileOutputStringBuilder.length()-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ExecuteMessage executeMessage = new ExecuteMessage();
        executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString());
        executeMessage.setMessage(inputCompileOutputStringBuilder.toString());
        executeMessage.setExitValue(exitValue);
        executeMessage.setTime(stopWatch.getTotalTimeMillis());
        return executeMessage;
    }
}
