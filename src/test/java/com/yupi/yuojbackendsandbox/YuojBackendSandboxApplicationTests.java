package com.yupi.yuojbackendsandbox;

import com.yupi.yuojbackendsandbox.model.ExecuteCodeRequest;
import com.yupi.yuojbackendsandbox.model.ExecuteCodeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

@SpringBootTest
class YuojBackendSandboxApplicationTests {
    @Resource
    private JavaNativeCodeSandBox javaNativeCodeSandBox;

    @Test
    void contextLoads() throws IOException {
        Process ls = Runtime.getRuntime().exec("ls");
        InputStream inputStream = ls.getInputStream();
        int c ;
        while ((c = inputStream.read())!=-1){
            System.out.print((char) c);
        }
    }

    @Test
    void test01(){
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest
                .builder()
                .code("public class Main {\n" +
                      "    public static void main(String[] args) {\n" +
                      "        Integer a = Integer.valueOf(args[0]);\n" +
                      "        Integer b = Integer.valueOf(args[1]);\n" +
                      "        System.out.println(\"结果：\" + (a + b));\n" +
                      "    }\n" +
                      "}")
                .language("java")
                .inputList(Arrays.asList("1 2", "2 3", "3 4"))
                .build();
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

}

