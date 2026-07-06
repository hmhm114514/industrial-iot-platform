package com.practice.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.OK) public ApiResponse<Object> handle(Exception e){ return ApiResponse.fail(e.getMessage()==null?"服务异常":e.getMessage()); }
}
