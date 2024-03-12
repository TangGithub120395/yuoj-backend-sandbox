package com.yupi.yuojbackendsandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/3 13:33
 */
public class WriteFileError {
    public static void main(String[] args) throws IOException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator
                          + "src" + File.separator
                          + "main" + File.separator
                          + "resources" + File.separator
                          + "application.yml";
        String errorProgram = "java -version 2>&1";
        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
        System.out.println("注入木马成功，你玩咯哈哈哈");
    }
}
