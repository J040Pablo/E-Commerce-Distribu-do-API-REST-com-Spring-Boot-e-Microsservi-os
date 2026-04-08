package com.ecommerce.api_gateway.error;

import com.ecommerce.api_gateway.filter.TraceIdGatewayFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Order(-2)
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayGlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(ex, status);
        String traceId = resolveTraceId(exchange);
        String path = exchange.getRequest().getURI().getPath();

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                traceId
        );

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(TraceIdGatewayFilter.TRACE_ID_HEADER, traceId);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            String fallback = "{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status.value()
                    + ",\"error\":\"" + status.getReasonPhrase() + "\",\"message\":\"" + message
                    + "\",\"path\":\"" + path + "\",\"traceId\":\"" + traceId + "\"}";
            bytes = fallback.getBytes(StandardCharsets.UTF_8);
        }

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
        }
        if (ex instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof ServerWebInputException || ex instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return status == HttpStatus.INTERNAL_SERVER_ERROR ? "Erro interno do servidor" : status.getReasonPhrase();
        }
        return ex.getMessage();
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        Object fromAttribute = exchange.getAttribute(TraceIdGatewayFilter.TRACE_ID_ATTRIBUTE);
        if (fromAttribute instanceof String traceId && !traceId.isBlank()) {
            return traceId;
        }
        String fromHeader = exchange.getRequest().getHeaders().getFirst(TraceIdGatewayFilter.TRACE_ID_HEADER);
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader;
        }
        return UUID.randomUUID().toString();
    }
}
