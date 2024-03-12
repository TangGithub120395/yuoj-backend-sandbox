package com.yupi.yuojbackendsandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeRequest;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeResponse;
import com.yupi.yuojbackendsandbox.model.ExecuteMessage;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/2 18:15
 */
@Component
public class JavaDockerCodeSandBox extends JavaCodeSandBoxTemplate{
    private static final long TIME_OUT = 5000L;

    private static Boolean FIRST_INIT = Boolean.TRUE;

    private static final Map<String, String> IMAGE_MAP;

    private static final DockerClient dockerClient;

    private String language;

    static {
        IMAGE_MAP = new HashMap<>();
        IMAGE_MAP.put("java", "openjdk:8-alpine");
        dockerClient = DockerClientBuilder.getInstance().build();
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        //1.1. 把用户代码保存为文件（使用HuTool工具类）（每个用户的代码文件都存储为Main.java，然后单独存放在一个目录当中，根据目录来区分代码）
        String code = executeCodeRequest.getCode();
        language = executeCodeRequest.getLanguage();
        List<String> inputList = executeCodeRequest.getInputList();
        //判断用户代码当中是否有敏感词
        File userCodeFile = super.saveCodeToFile(code);
        //2. 编译代码，得到class文件(使用java代码执行终端命令)
        ExecuteMessage compileFileExecuteMessage = super.compileFile(userCodeFile);
        //3.执行代码
        List<ExecuteMessage> runMessageList = runFile(userCodeFile, inputList);
        System.out.println("输出信息集合："+runMessageList);
        //4.整理数据
        ExecuteCodeResponse outputResponse = super.getOutputResponse(runMessageList);
        //5.清理文件
        boolean del = super.deleteFile(userCodeFile);
        return outputResponse;
    }

    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        List<ExecuteMessage> runMessageList = new ArrayList<>();
        //3拉取镜像
        String image = IMAGE_MAP.get(language);
        //判断镜像是否存在
        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
        List<Image> imageList = listImagesCmd.exec();
        for (Image tempImage : imageList) {
            if (tempImage.getRepoTags()[0].equals(image)) {
                FIRST_INIT = Boolean.FALSE;
            }
        }
        if (FIRST_INIT) {
            //拉取镜像
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("拉取镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
            System.out.println("拉取镜像 " + image + " 成功");
        } else {
            System.out.println("镜像" + image + "已经存在");
        }
        //4.创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        //设置每个容器的内存
        hostConfig.withMemory(100 * 1024 * 1024L);
        hostConfig.withMemorySwap(0L);
        //设置每个容器可以使用的CPU数
        hostConfig.withCpuCount(1L);
        //映射文件目录，将系统中的文件同步到容器当中
        hostConfig.setBinds(new Bind(userCodeFile.getParentFile().getAbsolutePath(), new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        //5.启动容器
        StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(containerId);
        startContainerCmd.exec();

        //6.在容器当中执行运行命令
        StopWatch stopWatch = new StopWatch();
        for (String input : inputList) {
            ExecuteMessage executeMessage = new ExecuteMessage();
            String[] inputArr = input.split(" ");
            //docker exec containerId java -cp /app Main 参数
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArr);
            ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(containerId);
            ExecCreateCmdResponse execCreateCmdResponse = execCreateCmd
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("执行创建命令: " + execCreateCmdResponse);
            String execId = execCreateCmdResponse.getId();
            ExecStartCmd execStartCmd = dockerClient.execStartCmd(execId);
            final boolean[] timeout = {false};
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {//如果没有超时就会执行这个方法
                    timeout[0] = true;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    if (StreamType.STDERR.equals(frame.getStreamType())) {
                        executeMessage.setErrorMessage(new String(frame.getPayload()));
                        System.out.println("异常输出结果：" + executeMessage.getErrorMessage());
                    } else {
                        executeMessage.setMessage(new String(frame.getPayload()));
                        System.out.println("正常输出结果：" + executeMessage.getMessage());
                    }
                    super.onNext(frame);
                }
            };

            //获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);

            final Long[] maxMemory = {0L};

            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });
//            statsCmd.exec(statisticsResultCallback);
            try {
                stopWatch.start();
                //执行
                execStartCmd
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                statsCmd.close();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executeMessage.setMemory(maxMemory[0]);
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
            runMessageList.add(executeMessage);
        }
        return runMessageList;
    }
}
