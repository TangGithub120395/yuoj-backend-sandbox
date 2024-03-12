package com.yupi.yuojbackendsandbox.security;

import cn.hutool.core.io.FileUtil;

import java.nio.charset.StandardCharsets;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/3 14:56
 */
public class Test {
    public static void main(String[] args) {
        System.setSecurityManager(new DefaultSecurityManager());
        FileUtil.writeString("ssss","ssss", StandardCharsets.UTF_8);
    }
}
