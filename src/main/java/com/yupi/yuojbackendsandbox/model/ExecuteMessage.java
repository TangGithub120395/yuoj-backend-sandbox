package com.yupi.yuojbackendsandbox.model;

import lombok.Data;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/2 18:58
 */
@Data
public class ExecuteMessage {

    private Integer exitValue;

    private String message;

    private String errorMessage;

    private Long time;

    private Long memory;
}

