package com.yupi.yuojbackendsandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.core.DockerClientBuilder;

import javax.xml.transform.Source;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/3 16:46
 */
public class DockerDemo {
    public static void main(String[] args) {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        PingCmd pingCmd = dockerClient.pingCmd();
        System.out.println(pingCmd);
    }
}
