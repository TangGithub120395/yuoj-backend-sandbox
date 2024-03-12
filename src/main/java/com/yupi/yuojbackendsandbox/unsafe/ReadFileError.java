package com.yupi.yuojbackendsandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/3 13:13
 */
public class ReadFileError {
    public static void main(String[] args) throws IOException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator
                          + "src" + File.separator
                          + "main" + File.separator
                          + "resources" + File.separator
                          + "application.yml";
        List<String> allLines = Files.readAllLines(Paths.get(filePath));
        for(String str : allLines){
            System.out.println(str);
        }
    }
}
