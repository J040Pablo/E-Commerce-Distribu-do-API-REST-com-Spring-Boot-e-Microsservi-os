package com.ecommerce.order_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.ecommerce.order_service.dto.ProductResponse;
import feign.codec.Decoder;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@Configuration
public class FeignConfig {

    @Bean
    public Decoder decoder(ObjectMapper objectMapper) {
        HttpMessageConverters converters = new HttpMessageConverters(
                new MappingJackson2HttpMessageConverter(objectMapper)
        );
        SpringDecoder decoder = new SpringDecoder(() -> converters);
        
        return new Decoder() {
            @Override
            public Object decode(feign.Response response, Type type) throws IOException {
                Object decoded = decoder.decode(response, Map.class);
                
                // If this is a ProductResponse request, extract from wrapper
                if (type == ProductResponse.class && decoded instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) decoded;
                    if (map.containsKey("data")) {
                        Object data = map.get("data");
                        return objectMapper.convertValue(data, ProductResponse.class);
                    }
                }
                
                return decoded;
            }
        };
    }
}
