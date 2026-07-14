package com.sungjujjang.entry.Global.Errors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@RequiredArgsConstructor
public class GolbalExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (BusinessException e){
            ErrorCode errorCode = e.getErrorCode();
            response.setStatus(errorCode.getErrorCode());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ErrorResponse errorResponse = ErrorResponse.errorCodeFrom(errorCode, errorCode.getErrorMessage());
            objectMapper.writeValue(response.getWriter(), errorResponse);
        } catch (Exception e){
            response.setStatus(response.getStatus());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ErrorResponse errorResponse = ErrorResponse.errorCodeOf(500, "INERNAL_SERVER_ERR", e.getMessage());
            objectMapper.writeValue(response.getWriter(), errorResponse);
        }
    }
}