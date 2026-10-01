package com.example.resilience.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class})
    ResponseEntity<ApiResponse<Void>> malformed(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_ARGUMENT", "请求参数、枚举值或必填请求头不合法"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> forbidden(Exception e) {
        return ResponseEntity.status(403).body(ApiResponse.error("FORBIDDEN", "没有操作权限"));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    ResponseEntity<ApiResponse<Void>> unauthorized(Exception e) {
        return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "账号或密码错误"));
    }

    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    ResponseEntity<ApiResponse<Void>> concurrency(Exception e) {
        return ResponseEntity.status(409).body(ApiResponse.error("CONCURRENT_UPDATE", "数据正在被处理，请稍后刷新再试"));
    }
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
        return ResponseEntity.status(e.status()).body(ApiResponse.error(e.code(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> invalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_ARGUMENT", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> constraint(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_ARGUMENT", e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> integrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DATA_CONFLICT", "数据冲突，请勿重复提交"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unknown(Exception e) {
        log.error("未处理的请求异常", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "服务暂时不可用"));
    }
}
