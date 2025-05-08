package com.example.common.dto;

/**
 * 通用响应对象
 * @param <T> 响应数据类型
 */
public class CommonResult<T> {
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 无参构造函数
     */
    public CommonResult() {
    }
    
    /**
     * 全参构造函数
     * @param code 响应码
     * @param message 响应消息
     * @param data 响应数据
     */
    public CommonResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 获取响应码
     * @return 响应码
     */
    public Integer getCode() {
        return code;
    }
    
    /**
     * 设置响应码
     * @param code 响应码
     */
    public void setCode(Integer code) {
        this.code = code;
    }
    
    /**
     * 获取响应消息
     * @return 响应消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 设置响应消息
     * @param message 响应消息
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * 获取响应数据
     * @return 响应数据
     */
    public T getData() {
        return data;
    }
    
    /**
     * 设置响应数据
     * @param data 响应数据
     */
    public void setData(T data) {
        this.data = data;
    }
    
    /**
     * 将当前类型转换为Void类型的CommonResult
     * @return CommonResult<Void>类型的实例
     */
    public CommonResult<Void> asVoid() {
        return new CommonResult<>(this.code, this.message, null);
    }
    
    /**
     * 成功响应
     * @param data 响应数据
     * @return 通用响应对象
     */
    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }
    
    /**
     * 成功响应
     * @param message 响应消息
     * @return 通用响应对象
     */
    public static CommonResult<String> success(String message) {
        CommonResult<String> result = new CommonResult<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(null);
        return result;
    }
    
    /**
     * 失败响应
     * @param code 响应码
     * @param message 响应消息
     * @return 通用响应对象
     */
    public static CommonResult<Void> failed(Integer code, String message) {
        CommonResult<Void> result = new CommonResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }
    
    /**
     * 失败响应
     * @param message 响应消息
     * @return 通用响应对象
     */
    public static CommonResult<Void> failed(String message) {
        return failed(500, message);
    }
} 