package com.example.exception;

import com.example.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

//建立一个全局异常处理器，用来处理不可知的运行时异常
@Slf4j//用来打印log的注解
//@RestControllerAdvice 是 Spring Framework 中的一个组合注解，
// 它相当于 @ControllerAdvice 与 @ResponseBody 的结合体，专门用于全局处理 RESTful API 的异常和响应增强。
@RestControllerAdvice
public class GlobalExceptionHandler {
    //处理业务异常（用户主动抛出的信息)
    @ExceptionHandler(CustomException.class)
    public Result handleCustomException(CustomException e){
        log.error("业务异常:", e);//打印堆栈日志方便后端排查
        return Result.error(e.getMessage());
    }
    //处理所有不可知的运行时异常（如数据库报错、空指针）
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e){
        log.error("系统异常:", e);//打印堆栈日志方便后端排查
        return Result.error("系统繁忙，请稍后再试。");
    }

    // 🔥 新增：专门处理 RestTemplate 调用外部 API 时的网络超时异常
    @ExceptionHandler(ResourceAccessException.class)
    public Result handleResourceAccessException(ResourceAccessException e) {
        log.error("AI接口网络异常:", e);
        Throwable cause = e.getCause();

        // 判断具体的超时类型
        if (cause instanceof SocketTimeoutException) {
            // 读取超时：通常是因为 AI 生成的内容太长，超过了我们在 Config 里设置的 120 秒
            return Result.error("AI老师思考时间过长（超时），请稍后重试哦~");
        } else if (cause instanceof ConnectException) {
            // 连接超时：通常是因为网络断开，或者连不上 DeepSeek 的服务器
            return Result.error("网络连接超时啦，请检查您的网络后重试~");
        }

        return Result.error("连接AI服务器失败，请稍后重试~");
    }



}
