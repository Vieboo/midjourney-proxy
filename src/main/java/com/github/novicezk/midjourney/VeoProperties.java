package com.github.novicezk.midjourney;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
public class VeoProperties {
    @Value("${ossAccessKey:vieboo}")
    private String ossAccessKey;
}
