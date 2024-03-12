package com.yupi.yuojbackendsandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeRequest;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeResponse;
import com.yupi.yuojbackendsandbox.model.ExecuteMessage;
import com.yupi.yuojbackendsandbox.model.JudgeInfo;
import com.yupi.yuojbackendsandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/2 18:15
 */
public class JavaNativeCodeSandBoxOld {
    private static final long TIME_OUT = 5000L;

    private static final List<String> blockList = Arrays.asList("Files","exec","File");

    private static final String SECURITY_MANAGER_PATH = System.getProperty("user.dir")
                     + File.separator + "src"
                     + File.separator + "main"
                     + File.separator + "resources"
                     + File.separator + "security";

    private static final WordTree WORD_TREE;

    static{
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blockList);
    }

    public static void main(String[] args) {
        JavaNativeCodeSandBoxOld javaNativeCodeSandBox = new JavaNativeCodeSandBoxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        String userDir = System.getProperty("user.dir");
        String fileDir = userDir
                         + File.separator + "src"
                         + File.separator + "main"
                         + File.separator + "resources"
                         + File.separator + "testCode"
                         + File.separator + "unsafeCode"
                         + File.separator + "Main.java";
        List<String> strings = FileUtil.readLines(fileDir, StandardCharsets.UTF_8);
        StringBuilder code = new StringBuilder();
        strings.forEach(str -> {
            code.append(str);
            code.append(System.lineSeparator());
        });
        executeCodeRequest.setCode(code.toString());
        executeCodeRequest.setLanguage("java");
        List<String> inputList = new ArrayList<>();
        inputList.add("1 2");
        inputList.add("2 3");
        executeCodeRequest.setInputList(inputList);

        try {
            ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBox.executeCode(executeCodeRequest);
            System.out.println(executeCodeResponse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        //1.1. 把用户代码保存为文件（使用HuTool工具类）（每个用户的代码文件都存储为Main.java，然后单独存放在一个目录当中，根据目录来区分代码）
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        List<String> inputList = executeCodeRequest.getInputList();
        //判断用户代码当中是否有敏感词
        FoundWord foundWord = WORD_TREE.matchWord(code);
//        if(foundWord != null){
//            System.out.println("包含敏感词："+foundWord.getFoundWord());
//            return null;
//        }
        //1.2. 创建存放所有用户代码的文件夹
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + "/tempCode";
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //1.3. 创建随机的单独目录(使用UUID)
        String userCodeParentPathName = globalCodePathName + File.separator + UUID.randomUUID().toString();
        FileUtil.mkdir(userCodeParentPathName);
        //1.4. 将用户代码存储为Main.java文件
        String userCodePathName = userCodeParentPathName + File.separator + "Main.java";
        File userCodeFile = FileUtil.writeString(code, userCodePathName, StandardCharsets.UTF_8);
        //2. 编译代码，得到class文件(使用java代码执行终端命令)
        String compileStr = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        System.out.println(compileStr);
        Process compileProcess = null;
        try {
            compileProcess = Runtime.getRuntime().exec(compileStr);
            ExecuteMessage executeCompileMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
        } catch (Exception e) {
            return getErrorResponse(e);
        }
        List<ExecuteMessage> runMessageList = new ArrayList<>();
        //3. 执行代码，得到输出结果
        for (String input : inputList) {
            try {
                String runStr = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s:%s -Djava.security.manager=DefaultSecurityManager Main %s", userCodeParentPathName, SECURITY_MANAGER_PATH,input);
                System.out.println(runStr);
                Process runProcess = Runtime.getRuntime().exec(runStr);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        System.out.println("程序超时，时间限制："+TIME_OUT);
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeRunMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                runMessageList.add(executeRunMessage);
            } catch (IOException e) {
                return getErrorResponse(e);
            }
        }
        //4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        //最大的时间
        Long maxTime = 0L;
        //注意：当有异常信息，就不会有正常信息
        for (ExecuteMessage executeMessage : runMessageList) {
            //如果有错误信息
            if (StrUtil.isNotBlank(executeMessage.getErrorMessage())) {
                executeCodeResponse.setMessage(executeMessage.getErrorMessage());
                //3:用户提交的代码有错误
                executeCodeResponse.setStatus(3);
                break;
            }
            //如果没有错误信息
            maxTime = Math.max(maxTime, executeMessage.getTime());
            outputList.add(executeMessage.getMessage());
        }
        if (outputList.size() == runMessageList.size()) {
            //这里表示代码执行成功了
            JudgeInfo judgeInfo = new JudgeInfo();
            executeCodeResponse.setOutputList(outputList);
            //1:用户提交的代码执行成功
            executeCodeResponse.setStatus(1);
            judgeInfo.setTime(maxTime);
            executeCodeResponse.setJudgeInfo(judgeInfo);
        }
//        5. 文件清理
//        if (userCodeFile.getParentFile().exists()) {
//            boolean del = FileUtil.del(userCodeParentPathName);
//            System.out.println("删除" + (del ? "成功" : "失败"));
//        }
        return executeCodeResponse;
    }

    //6. 错误处理，提升程序健壮性
    public ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setMessage(e.getMessage());
        //2:代码沙箱存在问题
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
