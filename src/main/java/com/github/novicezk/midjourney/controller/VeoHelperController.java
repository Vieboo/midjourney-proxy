package com.github.novicezk.midjourney.controller;

import cn.hutool.core.util.StrUtil;
import com.github.novicezk.midjourney.VeoProperties;
import com.github.novicezk.midjourney.util.OssUploadUtil;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
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


    @PostMapping("/stream")
    public ResponseEntity<byte[]>  steam(@RequestBody Map<String, Object> params) throws IOException {
        String urlStr = params.get("url").toString();
        if(StrUtil.isNotBlank(urlStr)) {
            log.info("------开始下载------");
            InputStream inputStream = null;
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-goog-api-key", properties.getGeminiKey());
                conn.connect();
                log.info("------返回状态码：" + conn.getResponseCode() + "------");
                if (conn.getResponseCode() != 200) {
                    log.info("------无法下载视频，响应码：" + conn.getResponseCode() + "------");
                    throw new RuntimeException("无法下载视频，响应码：" + conn.getResponseCode());
                }

//                inputStream = new BufferedInputStream(conn.getInputStream());
//                //读取视频数据
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                byte[] buffer = new byte[8192];
//                int len;
//                while ((len = inputStream.read(buffer)) != -1) {
//                    baos.write(buffer, 0, len);
//                }
//                // 返回视频流
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//                headers.setContentLength(baos.);
//                return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);

                byte[] data;

                try (InputStream in = conn.getInputStream();
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }

                    data = baos.toByteArray();
                }

                return ResponseEntity.ok()
                        .contentLength(data.length)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(data);

            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (ProtocolException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        return null;
    }

}
