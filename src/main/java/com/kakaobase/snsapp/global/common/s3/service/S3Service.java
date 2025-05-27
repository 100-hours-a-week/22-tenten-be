package com.kakaobase.snsapp.global.common.s3.service;

import com.kakaobase.snsapp.global.common.s3.dto.PresignedUrlResponseDto;
import com.kakaobase.snsapp.global.common.s3.exception.S3ErrorCode;
import com.kakaobase.snsapp.global.common.s3.exception.S3Exception;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * S3 관련 기능을 제공하는 서비스 클래스
 *
 * <p>이 클래스는 Amazon S3와의 상호작용을 담당하며, Presigned URL 생성,
 * 이미지 URL 검증, S3 객체 삭제 등의 기능을 제공합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    /**
     * S3 버킷 이름
     */
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * AWS 리전
     */
    @Value("${spring.cloud.aws.region.static}")
    private String region;

    /**
     * Presigned URL 만료 시간 (초)
     */
    @Value("${spring.cloud.aws.s3.expiration-time:300}")  // 기본값 5분
    private int expirationTime;

    /**
     * 최대 파일 크기 (바이트)
     */
    @Value("${spring.cloud.aws.s3.max-file-size:10485760}")  // 기본값 10MB
    private long maxFileSize;

    /**
     * 허용된 MIME 타입 목록
     */
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif"
    );

    /**
     * Presigned URL을 생성합니다.
     *
     * @param fileName 업로드할 파일명
     * @param fileSize 파일 크기 (바이트 단위)
     * @param mimeType 파일의 MIME 타입
     * @param type 이미지 사용 용도 (profile_image, post_image 등)
     * @return Presigned URL 정보가 포함된 응답 DTO
     * @throws S3Exception MIME 타입이 지원되지 않거나, 파일 크기가 제한을 초과하는 경우,
     *                   또는 S3 서비스 연결 오류 발생 시
     */
    public PresignedUrlResponseDto generatePresignedUrl(
            String fileName, Long fileSize, String mimeType, String type) {
        // 파일 타입 검증
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new S3Exception(S3ErrorCode.UNSUPPORTED_IMAGE_FORMAT);
        }

        // 파일 크기 검증
        if (fileSize > maxFileSize) {
            throw new S3Exception(S3ErrorCode.FILE_SIZE_EXCEEDED);
        }

        try {
            // 파일 경로 및 이름 생성 (타입에 따라 폴더 구분)
            String objectKey = generateObjectKey(type, fileName);

            // PutObjectRequest 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(mimeType)
                    .build();

            // Presigned PUT URL 생성 요청
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expirationTime))
                    .putObjectRequest(putObjectRequest)
                    .build();

            // Presigned URL 생성
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            URL presignedUrl = presignedRequest.url();

            // 이미지 접근 URL 생성
            String imageUrl = generateImageUrl(objectKey);

            return PresignedUrlResponseDto.builder()
                    .presigned_url(presignedUrl.toString())
                    .image_url(imageUrl)
                    .expires_in(expirationTime)
                    .build();
        } catch (Exception e) {
            log.error("S3 Presigned URL 생성 실패", e);
            throw new S3Exception(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 이미지 URL이 유효한지 확인합니다 (버킷 내 경로인지).
     *
     * @param imageUrl 확인할 이미지 URL
     * @return 유효한 URL이면 true, 그렇지 않으면 false
     */
    public boolean isValidImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return false;
        }

        String bucketDomain = bucketName + ".s3." + region + ".amazonaws.com";
        return imageUrl.contains(bucketDomain);
    }

    /**
     * S3에서 객체를 삭제합니다.
     *
     * @param imageUrl 삭제할 이미지의 URL
     * @throws S3Exception S3 객체 삭제 중 오류 발생 시
     */
    public void deleteObject(String imageUrl) {
        try {
            String objectKey = extractObjectKeyFromUrl(imageUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 객체 삭제 완료: {}", objectKey);
        } catch (Exception e) {
            log.error("S3 객체 삭제 실패: {}", imageUrl, e);
            throw new S3Exception(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 타입별 S3 객체 키를 생성합니다.
     *
     * @param type 이미지 타입 (profile_image, post_image 등)
     * @param originalFilename 원본 파일명
     * @return 생성된 객체 키
     */
    private String generateObjectKey(String type, String originalFilename) {
        // 확장자 추출
        String extension = getFileExtension(originalFilename);
        // UUID 생성
        String uuid = UUID.randomUUID().toString();

        // 타입에 따른 경로 설정
        String path;
        if ("profile_image".equals(type)) {
            path = "profiles";
        } else if ("post_image".equals(type)) {
            path = "post_image";
        }
        else {
            path = "others";
        }

        return String.format("%s/%s.%s", path, uuid, extension);
    }

    /**
     * 파일 확장자를 추출합니다.
     *
     * @param fileName 파일명
     * @return 파일 확장자
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "jpg";  // 기본 확장자
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 객체 키로부터 이미지 URL을 생성합니다.
     *
     * @param objectKey S3 객체 키
     * @return 이미지 접근 URL
     */
    private String generateImageUrl(String objectKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, objectKey);
    }

    /**
     * URL에서 S3 객체 키를 추출합니다.
     *
     * @param imageUrl 이미지 URL
     * @return 추출된 객체 키
     */
    private String extractObjectKeyFromUrl(String imageUrl) {
        String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/",
                bucketName, region);
        return imageUrl.substring(baseUrl.length());
    }
}