package com.kakaobase.snsapp.fixture.auth;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.fixture.common.AbstractFixture;

/**
 * CustomUserDetails 테스트 데이터 생성 Fixture
 */
public class CustomUserDetailsFixture extends AbstractFixture {

    /**
     * JWT 인증용 CustomUserDetails 생성 (기본 사용자)
     *
     * @return JWT 인증용 CustomUserDetails
     */
    public CustomUserDetails createDefaultJwtUser() {
        return new CustomUserDetails("1", "USER", "PANGYO_1", true);
    }

    /**
     * JWT 인증용 CustomUserDetails 생성 (커스텀 ID)
     *
     * @param id 사용자 ID
     * @return JWT 인증용 CustomUserDetails
     */
    public CustomUserDetails createJwtUserWithId(Long id) {
        return new CustomUserDetails(String.valueOf(id), "USER", "PANGYO_1", true);
    }

    /**
     * JWT 인증용 CustomUserDetails 생성 (전체 커스텀)
     *
     * @param id 사용자 ID
     * @param role 사용자 역할
     * @param className 기수
     * @return JWT 인증용 CustomUserDetails
     */
    public CustomUserDetails createJwtUser(Long id, String role, String className) {
        return new CustomUserDetails(String.valueOf(id), role, className, true);
    }

    /**
     * 로그인용 CustomUserDetails 생성 (기본 설정)
     *
     * @return 로그인용 CustomUserDetails
     */
    public CustomUserDetails createDefaultLoginUser() {
        return new CustomUserDetails(
                generateRandomEmail(),
                generateEncodedPassword(),
                "1",
                "USER",
                "PANGYO_1",
                generateRandomNickname(),
                generateRandomS3ImageUrl(),
                true
        );
    }

    /**
     * 로그인용 CustomUserDetails 생성 (커스텀 ID)
     *
     * @param id 사용자 ID
     * @return 로그인용 CustomUserDetails
     */
    public CustomUserDetails createLoginUserWithId(Long id) {
        String nickname = generateIndexedNickname(id.intValue());
        return new CustomUserDetails(
                generateEmailWithPrefix(nickname),
                generateEncodedPassword(),
                String.valueOf(id),
                "USER",
                "PANGYO_1",
                nickname,
                generateS3ImageUrl("profile" + id),
                true
        );
    }

    /**
     * 로그인용 CustomUserDetails 생성 (전체 커스텀)
     *
     * @param email 이메일
     * @param password 비밀번호
     * @param id 사용자 ID
     * @param role 사용자 역할
     * @param className 기수
     * @param nickname 닉네임
     * @param profileImgUrl 프로필 이미지 URL
     * @return 로그인용 CustomUserDetails
     */
    public CustomUserDetails createLoginUser(String email, String password, Long id,
                                             String role, String className,
                                             String nickname, String profileImgUrl) {
        return new CustomUserDetails(
                email, password, String.valueOf(id),
                role, className, nickname, profileImgUrl, true
        );
    }

    /**
     * 관리자 CustomUserDetails 생성 (JWT 인증용)
     *
     * @return 관리자 권한의 CustomUserDetails
     */
    public CustomUserDetails createAdminJwtUser() {
        return new CustomUserDetails("999", "ADMIN", "ALL", true);
    }

    /**
     * 관리자 CustomUserDetails 생성 (로그인용)
     *
     * @return 관리자 권한의 CustomUserDetails
     */
    public CustomUserDetails createAdminLoginUser() {
        return new CustomUserDetails(
                "admin@kakaobase.com",
                generateEncodedPassword(),
                "999",
                "ADMIN",
                "ALL",
                "admin",
                generateS3ImageUrl("admin"),
                true
        );
    }

    /**
     * 봇 CustomUserDetails 생성 (JWT 인증용)
     *
     * @return 봇 권한의 CustomUserDetails
     */
    public CustomUserDetails createBotJwtUser() {
        return new CustomUserDetails("1000", "BOT", "ALL", true);
    }

    /**
     * 비활성화된 CustomUserDetails 생성
     *
     * @return 비활성화된 CustomUserDetails
     */
    public CustomUserDetails createDisabledJwtUser() {
        return new CustomUserDetails("100", "USER", "PANGYO_1", false);
    }

    /**
     * 기수별 CustomUserDetails 생성
     *
     * @param className 기수
     * @return 지정된 기수의 CustomUserDetails
     */
    public CustomUserDetails createJwtUserWithClassName(String className) {
        return new CustomUserDetails("1", "USER", className, true);
    }

    /**
     * Member 엔티티로부터 JWT 인증용 CustomUserDetails 생성
     *
     * @param member Member 엔티티
     * @return JWT 인증용 CustomUserDetails
     */
    public CustomUserDetails createJwtUserFromMember(Member member) {
        return new CustomUserDetails(
                member.getId().toString(),
                member.getRole(),
                member.getClassName(),
                member.isEnabled()
        );
    }

    /**
     * Member 엔티티로부터 로그인용 CustomUserDetails 생성
     *
     * @param member Member 엔티티
     * @return 로그인용 CustomUserDetails
     */
    public CustomUserDetails createLoginUserFromMember(Member member) {
        return new CustomUserDetails(
                member.getEmail(),
                member.getPassword(),
                member.getId().toString(),
                member.getRole(),
                member.getClassName(),
                member.getNickname(),
                member.getProfileImgUrl(),
                member.isEnabled()
        );
    }

    /**
     * 다양한 기수의 사용자들 생성
     *
     * @return 각 기수별 CustomUserDetails 배열
     */
    public CustomUserDetails[] createUsersWithAllClasses() {
        return new CustomUserDetails[]{
                createJwtUser(1L, "USER", "PANGYO_1"),
                createJwtUser(2L, "USER", "PANGYO_2"),
                createJwtUser(3L, "USER", "JEJU_1"),
                createJwtUser(4L, "USER", "JEJU_2"),
                createJwtUser(5L, "USER", "JEJU_3")
        };
    }

    /**
     * 권한별 사용자들 생성
     *
     * @return 각 권한별 CustomUserDetails 배열
     */
    public CustomUserDetails[] createUsersWithAllRoles() {
        return new CustomUserDetails[]{
                createJwtUser(1L, "USER", "PANGYO_1"),
                createJwtUser(999L, "ADMIN", "ALL"),
                createJwtUser(1000L, "BOT", "ALL")
        };
    }

    @Override
    protected Object reset() {
        return this;
    }
}