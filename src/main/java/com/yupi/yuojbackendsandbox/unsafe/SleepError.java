package com.yupi.yuojbackendsandbox.unsafe;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/2 21:42
 */
public class SleepError {
    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(60 * 1000 * 60);
        System.out.println("睡完了");
    }
}
