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

    @Value("${ossSecretKey:vieboo}")
    private String ossSecretKey;

    @Value("${ossEndpoint:vieboo}")
    private String ossEndpoint;

    @Value("${ossBucketName:vieboo}")
    private String ossBucketName;

    @Value("${ossBucketNameTest:vieboo}")
    private String ossBucketNameTest;
}
