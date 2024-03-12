package com.yupi.yuojbackendsandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeRequest;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeResponse;
import com.yupi.yuojbackendsandbox.model.ExecuteMessage;
import com.yupi.yuojbackendsandbox.model.JudgeInfo;
import com.yupi.yuojbackendsandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class JavaCodeSandBoxTemplate implements CodeSandBox {
    private static final long TIME_OUT = 5000L;


    private static final String SECURITY_MANAGER_PATH = System.getProperty("user.dir")
                                                        + File.separator + "src"
                                                        + File.separator + "main"
                                                        + File.separator + "resources"
                                                        + File.separator + "security";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        List<String> inputList = executeCodeRequest.getInputList();
        //1.1. 把用户代码保存为文件（使用HuTool工具类）（每个用户的代码文件都存储为Main.java，然后单独存放在一个目录当中，根据目录来区分代码）
        File userCodeFile = saveCodeToFile(code);
        //2. 编译代码，得到class文件(使用java代码执行终端命令)
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);
        //3. 执行代码，得到输出结果
        List<ExecuteMessage> runMessageList = runFile(userCodeFile,inputList);
        //4. 收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(runMessageList);
        //5. 文件清理
        boolean del = deleteFile(userCodeFile);
        System.out.println("删除" + (del ? "成功" : "失败"));
        return outputResponse;
    }


    /**
     * 将用户提供的代码保存为一个文件
     *
     * @param code
     * @return
     */
    public File saveCodeToFile(String code) {
        //1.2. 创建存放所有用户代码的文件夹
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + "tempCode";
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //1.3. 创建随机的单独目录(使用UUID)
        String userCodeParentPathName = globalCodePathName + File.separator + UUID.randomUUID();
        FileUtil.mkdir(userCodeParentPathName);
        //1.4. 将用户代码存储为Main.java文件
        String userCodePathName = userCodeParentPathName + File.separator + "Main.java";
        File userCodeFile = FileUtil.writeString(code, userCodePathName, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 将保存好的代码文件进行编译
     *
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileStr = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        System.out.println(compileStr);
        Process compileProcess = null;
        try {
            compileProcess = Runtime.getRuntime().exec(compileStr);
            ExecuteMessage executeCompileMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            if (executeCompileMessage.getExitValue() != 0) {
                throw new RuntimeException("编译操作错误");
            }
            return executeCompileMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行编译好的java文件
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile,List<String> inputList) {
        List<ExecuteMessage> runMessageList = new ArrayList<>();
        for (String input : inputList) {
            try {
                String runStr = String.format(
                        "java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s",
                        userCodeFile.getParentFile().getAbsolutePath(),
                        input);
                System.out.println(runStr);
                Process runProcess = Runtime.getRuntime().exec(runStr);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();
                        System.out.println("程序超时，时间限制：" + TIME_OUT);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeRunMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                runMessageList.add(executeRunMessage);
            } catch (IOException e) {
                throw new RuntimeException("程序执行异常");
            }
        }
        return runMessageList;
    }

    /**
     * 根据执行java文件的输出信息，整理信息
     * @param runMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> runMessageList) {
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
        return executeCodeResponse;
    }

    /**
     * 删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile){
        boolean del = true;
        if (userCodeFile.getParentFile().exists()) {
            del = FileUtil.del(userCodeFile.getParentFile().getAbsolutePath());
        }
        return del;
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
