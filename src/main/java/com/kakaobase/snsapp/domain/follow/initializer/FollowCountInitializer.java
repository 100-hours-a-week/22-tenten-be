package com.kakaobase.snsapp.domain.follow.initializer;

import com.kakaobase.snsapp.domain.members.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class FollowCountInitializer implements ApplicationRunner {

    private final MemberService memberService;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("🚀 애플리케이션 시작 - 팔로우 카운트 동기화 시작...");

        try {
            memberService.syncMemberFollowCount();
        } catch (Exception e) {
            System.err.println("❌ 팔로우 카운트 동기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
