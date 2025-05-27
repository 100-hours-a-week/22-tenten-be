package com.kakaobase.snsapp.global.factory;

import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * 테스트용 Member 객체를 생성하는 팩토리 클래스
 */
@Component
public class TestMemberFactory {

    private final PasswordEncoder passwordEncoder;

    public TestMemberFactory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 카테부 멤버 생성
     */
    public Member createKtbMember() {
        return Member.builder()
                .email("ktb"+UUID.randomUUID()+"@example.com")
                .name("테스트유저")
                .nickname("testuser")
                .password(passwordEncoder.encode("Test1234!"))
                .className(Member.ClassName.PANGYO_1)
                .githubUrl("https://github.com/testuser")
                .build();
    }

    /**
     * 비 카테부 멤버 생성
     */
    public Member createNonKtbMember() {
        return Member.builder()
                .email("nonKtb"+UUID.randomUUID()+"@example.com")
                .name("테스트유저")
                .nickname("testuser")
                .password(passwordEncoder.encode("Test1234!"))
                .className(Member.ClassName.ALL)
                .githubUrl("https://github.com/testuser")
                .build();
    }

    /**
     * 밴 상태인 멤버 생성
     */
    public Member createBannedMember() {
        Member member = createNonKtbMember();
        member.updateBanStatus(true);
        return member;
    }

    /**
     * 여러 멤버를 한 번에 생성
     */
    public List<Member> createMultipleMembers(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> Member.builder()
                        .email("test" + i + "@example.com")
                        .name("테스트유저" + i)
                        .nickname("testuser" + i)
                        .password(passwordEncoder.encode("Test1234!"))
                        .className(Member.ClassName.values()[i % Member.ClassName.values().length])
                        .githubUrl("https://github.com/testuser" + i)
                        .build())
                .toList();
    }
}