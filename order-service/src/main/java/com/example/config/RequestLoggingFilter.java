package com.example.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RequestLoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        logger.info("Incoming request: {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
        logger.debug("Content-Type: {}", httpRequest.getContentType());
        logger.debug("Headers: {}", httpRequest.getHeaderNames());

        chain.doFilter(request, response);
    }
}
