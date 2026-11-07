package com.example.common;

import lombok.Data;

//Result是一个统一的对象，这个对象才可以访问JSON，因为在SpringMVC里面，会把controller接口返回的对象渲染成一个JSON，把接口返回的list渲染成一个JSON数组，
@Data//这个注解可以给类的所有属性加上get和set
public class Result {
    private static final String SUCCESS_CODE = "200";
    private static final String ERROR_CODE = "500";
    private String code;//状态码，是成功还是失败；200表示成功，500表示服务器错误
    private String msg;//错误信息
    private Object data;
    //如果不写@Data的话就只能alt+insert自己进行导入
    public static Result success(){
        Result result = new Result();
        result.setCode(SUCCESS_CODE);
        result.setMsg("请求成功");
        return result;
    }

    public static Result success(Object data){
        Result result = success();
        result.setData(data);
        return result;
    }

    public static Result error(String msg){
        Result result = new Result();
        result.setCode(ERROR_CODE);
        result.setMsg(msg);
        return result;
    }

    // 🔥 新增：同时包含提示信息和数据的方法
    public static Result success(String msg, Object data) {
        Result result = new Result();
        result.setCode("200"); // 或者你定义的成功状态码
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

}
