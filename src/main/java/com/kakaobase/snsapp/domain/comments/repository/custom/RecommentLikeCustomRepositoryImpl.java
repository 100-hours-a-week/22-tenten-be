package com.kakaobase.snsapp.domain.comments.repository.custom;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.kakaobase.snsapp.domain.members.entity.QMember.member;
import static com.kakaobase.snsapp.domain.comments.entity.QRecommentLike.recommentLike;

@Repository
@RequiredArgsConstructor
public class RecommentLikeCustomRepositoryImpl implements RecommentLikeCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberResponseDto.UserInfo> findMembersByRecommentIdWithCursor(
            Long recommentId,
            Long lastMemberId,
            int limit) {

        return queryFactory
                .select(Projections.constructor(
                        MemberResponseDto.UserInfo.class,
                        member.id,
                        member.name,
                        member.nickname,
                        member.profileImgUrl
                ))
                .from(recommentLike)
                .join(recommentLike.member, member)
                .where(
                        recommentLike.recomment.id.eq(recommentId)
                                .and(lastMemberId != null ? member.id.lt(lastMemberId) : null)  // 커서 조건
                )
                .orderBy(member.id.desc())  // ID 내림차순 정렬
                .limit(limit)
                .fetch();
    }
}