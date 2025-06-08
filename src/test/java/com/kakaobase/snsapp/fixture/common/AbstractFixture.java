package com.kakaobase.snsapp.fixture.common;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * 모든 Fixture 클래스의 기본이 되는 추상 클래스
 *
 * 제공 기능:
 * - 공통 유틸리티 메서드
 * - 랜덤 데이터 생성 메서드
 * - 시간 관련 테스트 유틸리티
 * - 리플렉션 기반 필드 설정 도우미
 */
public abstract class AbstractFixture {

    // === 상수 정의 ===
    protected static final Random RANDOM = new Random(12345L); // 시드 고정으로 재현 가능한 테스트
    protected static final TestTimeProvider TIME_PROVIDER = new TestTimeProvider();

    // 기본 도메인 상수
    protected static final String DEFAULT_DOMAIN = "example.com";
    protected static final String DEFAULT_GITHUB_DOMAIN = "https://github.com";
    protected static final String DEFAULT_S3_DOMAIN = "https://s3.amazonaws.com/bucket";

    // 문자열 생성용 문자 집합
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String KOREAN_NAMES = "김이박최정강조윤장임";
    private static final String[] FIRST_NAMES = {"민준", "서연", "도윤", "하은", "시우", "하윤", "주원", "지유"};

    // === 랜덤 데이터 생성 메서드 ===

    /**
     * 지정된 길이의 랜덤 영숫자 문자열 생성
     *
     * @param length 생성할 문자열 길이
     * @return 랜덤 영숫자 문자열
     */
    protected static String generateRandomString(int length) {
        return RANDOM.ints(length, 0, ALPHANUMERIC.length())
                .mapToObj(ALPHANUMERIC::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    /**
     * 지정된 접두사를 가진 랜덤 문자열 생성
     *
     * @param prefix 접두사
     * @param randomLength 랜덤 부분 길이
     * @return 접두사 + 랜덤 문자열
     */
    protected static String generateRandomStringWithPrefix(String prefix, int randomLength) {
        return prefix + generateRandomString(randomLength);
    }

    /**
     * 랜덤 Long ID 생성 (1 ~ 999999)
     *
     * @return 랜덤 Long ID
     */
    protected static Long generateRandomId() {
        return ThreadLocalRandom.current().nextLong(1L, 1000000L);
    }

    /**
     * 지정된 범위의 랜덤 Long 생성
     *
     * @param min 최소값 (포함)
     * @param max 최대값 (제외)
     * @return 랜덤 Long 값
     */
    protected static Long generateRandomLong(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max);
    }

    /**
     * 지정된 범위의 랜덤 Integer 생성
     *
     * @param min 최소값 (포함)
     * @param max 최대값 (제외)
     * @return 랜덤 Integer 값
     */
    protected static Integer generateRandomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    /**
     * 랜덤 boolean 값 생성
     *
     * @return 랜덤 boolean
     */
    protected static boolean generateRandomBoolean() {
        return RANDOM.nextBoolean();
    }

    /**
     * 지정된 확률로 true를 반환하는 boolean 생성
     *
     * @param probability true가 나올 확률 (0.0 ~ 1.0)
     * @return boolean 값
     */
    protected static boolean generateRandomBooleanWithProbability(double probability) {
        return RANDOM.nextDouble() < probability;
    }

    // === 시간 관련 메서드 ===

    /**
     * 현재 시간 반환 (테스트용 고정 시간 가능)
     *
     * @return 현재 LocalDateTime
     */
    protected static LocalDateTime now() {
        return TIME_PROVIDER.now();
    }

    /**
     * 현재 시간 기준 N일 전 시간 반환
     *
     * @param days 일수
     * @return N일 전 LocalDateTime
     */
    protected static LocalDateTime daysAgo(long days) {
        return TIME_PROVIDER.now().minusDays(days);
    }

    /**
     * 현재 시간 기준 N일 후 시간 반환
     *
     * @param days 일수
     * @return N일 후 LocalDateTime
     */
    protected static LocalDateTime daysAfter(long days) {
        return TIME_PROVIDER.now().plusDays(days);
    }

    /**
     * 현재 시간 기준 N분 전 시간 반환
     *
     * @param minutes 분수
     * @return N분 전 LocalDateTime
     */
    protected static LocalDateTime minutesAgo(long minutes) {
        return TIME_PROVIDER.now().minusMinutes(minutes);
    }

    /**
     * 지정된 범위 내의 랜덤 시간 생성
     *
     * @param startDateTime 시작 시간
     * @param endDateTime 종료 시간
     * @return 랜덤 LocalDateTime
     */
    protected static LocalDateTime generateRandomDateTime(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        long startEpoch = startDateTime.toEpochSecond(java.time.ZoneOffset.UTC);
        long endEpoch = endDateTime.toEpochSecond(java.time.ZoneOffset.UTC);
        long randomEpoch = ThreadLocalRandom.current().nextLong(startEpoch, endEpoch);
        return LocalDateTime.ofEpochSecond(randomEpoch, 0, java.time.ZoneOffset.UTC);
    }

    // === 이메일 생성 메서드 ===

    /**
     * 랜덤 이메일 주소 생성
     *
     * @return 랜덤 이메일 주소
     */
    protected static String generateRandomEmail() {
        return generateRandomString(8).toLowerCase() + "@" + DEFAULT_DOMAIN;
    }

    /**
     * 지정된 접두사를 가진 이메일 주소 생성
     *
     * @param prefix 이메일 접두사
     * @return 접두사를 가진 이메일 주소
     */
    protected static String generateEmailWithPrefix(String prefix) {
        return prefix + "@" + DEFAULT_DOMAIN;
    }

    /**
     * 순서가 있는 이메일 주소 생성
     *
     * @param index 순서 번호
     * @return 순서가 있는 이메일 주소 (user1@example.com 형태)
     */
    protected static String generateIndexedEmail(int index) {
        return "user" + index + "@" + DEFAULT_DOMAIN;
    }

    // === 한국어 이름 생성 메서드 ===

    /**
     * 랜덤 한국어 이름 생성
     *
     * @return 랜덤 한국어 이름
     */
    protected static String generateRandomKoreanName() {
        char lastName = KOREAN_NAMES.charAt(RANDOM.nextInt(KOREAN_NAMES.length()));
        String firstName = FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
        return lastName + firstName;
    }

    /**
     * 순서가 있는 한국어 이름 생성
     *
     * @param index 순서 번호
     * @return 순서가 있는 한국어 이름 (테스트유저1 형태)
     */
    protected static String generateIndexedKoreanName(int index) {
        return "테스트유저" + index;
    }

    // === 닉네임 생성 메서드 ===

    /**
     * 랜덤 닉네임 생성
     *
     * @return 랜덤 닉네임
     */
    protected static String generateRandomNickname() {
        return "user" + generateRandomString(6).toLowerCase();
    }

    /**
     * 순서가 있는 닉네임 생성
     *
     * @param index 순서 번호
     * @return 순서가 있는 닉네임 (user1 형태)
     */
    protected static String generateIndexedNickname(int index) {
        return "user" + index;
    }

    // === URL 생성 메서드 ===

    /**
     * 랜덤 GitHub URL 생성
     *
     * @return 랜덤 GitHub URL
     */
    protected static String generateRandomGithubUrl() {
        return DEFAULT_GITHUB_DOMAIN + "/" + generateRandomString(8).toLowerCase();
    }

    /**
     * 지정된 사용자명으로 GitHub URL 생성
     *
     * @param username GitHub 사용자명
     * @return GitHub URL
     */
    protected static String generateGithubUrl(String username) {
        return DEFAULT_GITHUB_DOMAIN + "/" + username;
    }

    /**
     * 랜덤 S3 이미지 URL 생성
     *
     * @return 랜덤 S3 이미지 URL
     */
    protected static String generateRandomS3ImageUrl() {
        return DEFAULT_S3_DOMAIN + "/images/" + generateRandomString(16) + ".jpg";
    }

    /**
     * 지정된 파일명으로 S3 이미지 URL 생성
     *
     * @param filename 파일명 (확장자 제외)
     * @return S3 이미지 URL
     */
    protected static String generateS3ImageUrl(String filename) {
        return DEFAULT_S3_DOMAIN + "/images/" + filename + ".jpg";
    }

    // === 비밀번호 생성 메서드 ===

    /**
     * 유효한 비밀번호 생성 (영문, 숫자, 특수문자 포함)
     *
     * @return 유효한 비밀번호
     */
    protected static String generateValidPassword() {
        return "Test" + generateRandomInt(1000, 9999) + "!";
    }

    /**
     * 암호화된 비밀번호 시뮬레이션 (실제로는 BCrypt 등으로 암호화)
     *
     * @return 암호화된 비밀번호 모형
     */
    protected static String generateEncodedPassword() {
        return "$2a$10$" + generateRandomString(53); // BCrypt 형태 모방
    }

    // === 리스트 생성 메서드 ===

    /**
     * 지정된 개수만큼 인덱스 기반 리스트 생성
     *
     * @param count 생성할 개수
     * @param generator 각 인덱스에 대한 객체 생성 함수
     * @param <T> 생성할 객체 타입
     * @return 생성된 객체 리스트
     */
    protected static <T> List<T> generateIndexedList(int count, java.util.function.IntFunction<T> generator) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(generator)
                .toList();
    }

    /**
     * 지정된 배열에서 랜덤 요소 선택
     *
     * @param array 선택할 배열
     * @param <T> 배열 요소 타입
     * @return 랜덤 선택된 요소
     */
    @SafeVarargs
    protected static <T> T selectRandom(T... array) {
        return array[RANDOM.nextInt(array.length)];
    }

    /**
     * 지정된 리스트에서 랜덤 요소 선택
     *
     * @param list 선택할 리스트
     * @param <T> 리스트 요소 타입
     * @return 랜덤 선택된 요소
     */
    protected static <T> T selectRandom(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    // === 리플렉션 유틸리티 메서드 ===

    /**
     * 리플렉션을 사용하여 객체의 ID 필드 설정
     *
     * @param target 대상 객체
     * @param id 설정할 ID
     */
    protected static void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }

    /**
     * 리플렉션을 사용하여 객체의 임의 필드 설정
     *
     * @param target 대상 객체
     * @param fieldName 필드명
     * @param value 설정할 값
     */
    protected static void setField(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

    /**
     * 리플렉션을 사용하여 객체의 createdAt 필드 설정
     *
     * @param target 대상 객체
     * @param createdAt 설정할 생성 시간
     */
    protected static void setCreatedAt(Object target, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(target, "createdAt", createdAt);
    }

    /**
     * 리플렉션을 사용하여 객체의 updatedAt 필드 설정
     *
     * @param target 대상 객체
     * @param updatedAt 설정할 수정 시간
     */
    protected static void setUpdatedAt(Object target, LocalDateTime updatedAt) {
        ReflectionTestUtils.setField(target, "updatedAt", updatedAt);
    }

    // === 빌더 패턴 지원 메서드 ===

    /**
     * 빌더 리셋을 위한 메서드 (하위 클래스에서 구현)
     *
     * @return 리셋된 빌더 인스턴스
     */
    protected abstract Object reset();

    // === 디버깅 및 로깅 메서드 ===

    /**
     * 현재 시간을 포맷된 문자열로 반환
     *
     * @return 포맷된 현재 시간 문자열
     */
    protected static String getCurrentTimeString() {
        return TIME_PROVIDER.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 테스트용 로그 메시지 생성
     *
     * @param message 로그 메시지
     * @return 시간 정보가 포함된 로그 메시지
     */
    protected static String createLogMessage(String message) {
        return "[" + getCurrentTimeString() + "] " + message;
    }
}