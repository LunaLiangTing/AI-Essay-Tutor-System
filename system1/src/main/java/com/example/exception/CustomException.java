package com.example.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
//自定义的异常，必须要继承RuntimeException否则没法把它定义成一个异常的类了；RuntimeException表示在运行时检查异常
public class CustomException extends RuntimeException{
//    private String code;
//    private String msg;
//
//    public CustomException(String code, String msg) {
//        this.code = code;
//        this.msg = msg;
//    }
//
//    public CustomException(String msg) {
//        this.code = "500";
//        this.msg = msg;
//    }
    public CustomException(String message){
        super(message);
    }
}
