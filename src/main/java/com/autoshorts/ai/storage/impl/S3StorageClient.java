package com.autoshorts.ai.storage.impl;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.storage.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.nio.file.Path;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.storage.mock", havingValue = "false", matchIfMissing = true)
public class S3StorageClient implements StorageClient {

    private static final Logger log = LoggerFactory.getLogger(S3StorageClient.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AppProperties appProperties;

    public S3StorageClient(S3Client s3Client, S3Presigner s3Presigner, AppProperties appProperties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.appProperties = appProperties;
        ensureBucketExists();
    }

    @Override
    public String upload(Path sourceFile, String objectKey, String contentType) {
        String bucket = appProperties.getStorage().getBucket();
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
            .build();

        try {
            s3Client.putObject(request, RequestBody.fromFile(sourceFile));
            String url = resolveAssetUrl(objectKey);
            log.info("event=storage_upload bucket={} key={} contentType={} url={}", bucket, objectKey, contentType, url);
            return url;
        } catch (Exception ex) {
            throw new IllegalStateException(
                "Failed to upload to S3-compatible storage bucket=%s key=%s".formatted(bucket, objectKey), ex
            );
        }
    }

    private String resolveAssetUrl(String objectKey) {
        String publicBaseUrl = appProperties.getStorage().getPublicBaseUrl();
        if (StringUtils.hasText(publicBaseUrl)) {
            return stripTrailingSlash(publicBaseUrl) + "/" + objectKey;
        }

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(appProperties.getStorage().getPresignDurationSeconds()))
                .getObjectRequest(r -> r
                    .bucket(appProperties.getStorage().getBucket())
                    .key(objectKey))
                .build()
        );

        return presigned.url().toString();
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private void ensureBucketExists() {
        String bucket = appProperties.getStorage().getBucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException ex) {
            createBucket(bucket);
        } catch (AwsServiceException ex) {
            if (ex.statusCode() == 404) {
                createBucket(bucket);
            } else {
                log.warn("event=s3_head_bucket_failed bucket={} status={} message={}",
                    bucket, ex.statusCode(), ex.getMessage());
            }
        } catch (Exception ex) {
            log.warn("event=s3_bucket_check_failed bucket={} message={}", bucket, ex.getMessage());
        }
    }

    private void createBucket(String bucket) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("event=s3_bucket_created bucket={}", bucket);
        } catch (Exception createEx) {
            log.warn("event=s3_bucket_create_failed bucket={} message={}", bucket, createEx.getMessage());
        }
    }
}
