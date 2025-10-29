package com.github.novicezk.midjourney;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "veo")
public class VeoProperties {
    private String ossAccessKey;
}
