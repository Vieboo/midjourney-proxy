package com.github.novicezk.midjourney.controller;

import cn.hutool.core.util.StrUtil;
import com.github.novicezk.midjourney.VeoProperties;
import com.github.novicezk.midjourney.util.OssUploadUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "veoHelper")
@RestController
@RequestMapping("/veo")
@RequiredArgsConstructor
public class VeoHelperController {

    @Autowired
    private OssUploadUtil ossUploadUtil;
    @Autowired
    @Lazy
    private VeoProperties properties;

    @ApiOperation(value = "test")
    @PostMapping("/test/uploadVideo")
    public Map<String, Object> testUploadVideo(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        String url = params.get("url").toString();
        String filePath = params.get("filePath").toString();
        if(StrUtil.isNotBlank(url)) {
            Map<String, String> headers = new HashMap<>();
            headers.put("x-goog-api-key", properties.getGeminiKey());
            String uploadUrl = ossUploadUtil.uploadFromUrl(url, filePath, headers, true);
            result.put("uploadUrl", uploadUrl);
        }
        return result;
    }

    @ApiOperation(value = "test")
    @PostMapping("/uploadVideo")
    public Map<String, Object> uploadVideo(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        String url = params.get("url").toString();
        String filePath = params.get("filePath").toString();
        if(StrUtil.isNotBlank(url)) {
            Map<String, String> headers = new HashMap<>();
            headers.put("x-goog-api-key", properties.getGeminiKey());
            String uploadUrl = ossUploadUtil.uploadFromUrl(url, filePath, headers, false);
            result.put("uploadUrl", uploadUrl);
        }
        return result;
    }

}
