package com.github.novicezk.midjourney.util;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.*;
import com.github.novicezk.midjourney.VeoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class OssUploadUtil {

    @Autowired
    private VeoProperties veoProperties;

    private long urlExpireSeconds = 3600;


    private OSS getClient() {
        return new OSSClient(veoProperties.getOssEndpoint(),
                new DefaultCredentialProvider(veoProperties.getOssAccessKey(), veoProperties.getOssSecretKey()),
                new ClientConfiguration());
    }

    /**
     * 上传本地文件
     */
    public String uploadFile(File file, String folder, boolean isTest) {
        String bucketName = isTest ? veoProperties.getOssBucketNameTest() : veoProperties.getOssBucketName();
        String objectName = generateObjectName(folder, file.getName());
        OSS ossClient = getClient();
        try {
            ossClient.putObject(bucketName, objectName, file);
            return objectName;
        } catch (OSSException | ClientException e) {
            throw new RuntimeException("OSS 上传文件失败：" + e.getMessage(), e);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 上传输入流（常用于前端上传）
     */
    public String uploadStream(InputStream inputStream, String fileName, String folder, boolean isTest) {
        String bucketName = isTest ? veoProperties.getOssBucketNameTest() : veoProperties.getOssBucketName();
        String objectName = generateObjectName(folder, fileName);
        OSS ossClient = getClient();
        try {
            ossClient.putObject(bucketName, objectName, inputStream);
            return objectName;
        } catch (OSSException | ClientException e) {
            throw new RuntimeException("OSS 上传流失败：" + e.getMessage(), e);
        } finally {
            ossClient.shutdown();
        }
    }


    /**
     * 从 URL 下载视频并上传到 OSS
     *
     * @param videoUrl 网络视频地址
     * @param filePath OSS 上传目录
     * @return 上传后的 OSS 访问地址
     */
    public String uploadFromUrl(String videoUrl, String filePath, Map<String, String> headers, boolean isTest) {
        String bucketName = isTest ? veoProperties.getOssBucketNameTest() : veoProperties.getOssBucketName();
        OSS ossClient = null;
        InputStream inputStream = null;

        log.info("------开始下载------");

        try {
            // 打开连接
            URL url = new URL(videoUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);
            conn.setRequestMethod("GET");

            // 添加自定义 Header
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            conn.connect();

            log.info("------返回状态码：" + conn.getResponseCode() + "------");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("无法下载视频，响应码：" + conn.getResponseCode());
            }

            inputStream = new BufferedInputStream(conn.getInputStream());

            // 生成 OSS 文件名
//            String fileName = extractFileName(videoUrl);
//            String objectName = generateObjectName(folder, fileName);
            String objectName = filePath;

            log.info("------oss参数：bucket：" + (bucketName) + "，filePath：" + objectName + "------");

            // 上传到 OSS
            ossClient = getClient();
            byte[] bytes = inputStream.readAllBytes();
            log.info("------文件流大小：" + bytes.length + "------");
            InputStream uploadStream = new ByteArrayInputStream(bytes);
            PutObjectResult result = ossClient.putObject(bucketName, objectName, uploadStream);
            ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);

            log.info("------oss上传完成------");

            // 生成访问 URL
//            Date expiration = new Date(System.currentTimeMillis() + urlExpireSeconds * 1000);
//            String fileUrl = ossClient.generatePresignedUrl(bucketName, objectName, expiration).toString();

            return filePath;

        } catch (IOException e) {
            log.error("OSS上传失败：" + e.getMessage());
            throw new RuntimeException("从 URL 上传视频到 OSS 失败: " + e.getMessage(), e);
        } finally {
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException ignored) {}
            }
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 从 URL 中提取文件名（如 video.mp4）
     */
    private String extractFileName(String url) {
        String name = url.substring(url.lastIndexOf('/') + 1);
        if (name.isEmpty()) {
            name = "video.mp4";
        }
        return name;
    }


    /**
     * 分片上传（适合大文件）
     */
    public String multipartUpload(File file, String folder, boolean isTest) {
        String bucketName = isTest ? veoProperties.getOssBucketNameTest() : veoProperties.getOssBucketName();
        String objectName = generateObjectName(folder, file.getName());
        OSS ossClient = getClient();
        try {
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectName);
            InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
            String uploadId = result.getUploadId();

            final long partSize = 5 * 1024 * 1024L; // 5MB 分片
            long fileLength = file.length();
            int partCount = (int) Math.ceil((double) fileLength / partSize);

            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, objectName, uploadId);
            for (int i = 0; i < partCount; i++) {
                long startPos = i * partSize;
                long curPartSize = Math.min(partSize, fileLength - startPos);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(objectName);
                uploadPartRequest.setUploadId(uploadId);
//                uploadPartRequest.setFile(file);
                uploadPartRequest.setPartSize(curPartSize);
                uploadPartRequest.setPartNumber(i + 1);
//                uploadPartRequest.setFileOffset(startPos);
                ossClient.uploadPart(uploadPartRequest);
            }

            // 完成上传
//            CompleteMultipartUploadRequest completeRequest =
//                    new CompleteMultipartUploadRequest(bucketName,
//                            objectName, uploadId, ossClient.listParts(listPartsRequest).getParts());
//            ossClient.completeMultipartUpload(completeRequest);
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("OSS 分片上传失败：" + e.getMessage(), e);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 生成带签名访问 URL（适合私有文件）
     */
    public String generatePresignedUrl(String objectName, boolean isTest) {
        String bucketName = isTest ? veoProperties.getOssBucketNameTest() : veoProperties.getOssBucketName();
        OSS ossClient = getClient();
        try {
            Date expiration = new Date(System.currentTimeMillis() + urlExpireSeconds * 1000);
            URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
            return url.toString();
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName, boolean isTest) {
        String bucketName = isTest ? veoProperties.getOssBucketNameTest() : veoProperties.getOssBucketName();
        OSS ossClient = getClient();
        try {
            ossClient.deleteObject(bucketName, objectName);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 构造文件名（路径 + UUID + 原文件名）
     */
    private String generateObjectName(String folder, String originalName) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (folder != null && !folder.endsWith("/")) {
            folder = folder + "/";
        }
        return folder + uuid + "_" + originalName;
    }
}