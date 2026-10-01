package com.vone.mq.controller;

import com.vone.mq.dto.CommonRes;
import com.vone.mq.utils.ResUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获控制器抛出的异常，按系统既有的 CommonRes 结构返回 JSON，
 * 避免异常时直接暴露 Spring 默认错误结构。
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 参数类异常：直接把异常信息返回给调用方。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public CommonRes handleIllegalArgument(HttpServletRequest request, IllegalArgumentException e) {
        log.warn("参数异常, uri={}, msg={}", request.getRequestURI(), e.getMessage());
        return ResUtil.error(e.getMessage());
    }

    /**
     * 兜底异常：记录完整堆栈，对调用方返回统一提示。
     */
    @ExceptionHandler(Exception.class)
    public CommonRes handleException(HttpServletRequest request, Exception e) {
        log.error("系统异常, uri={}", request.getRequestURI(), e);
        return ResUtil.error("系统繁忙，请稍后再试");
    }
}
