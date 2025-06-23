package com.kakaobase.snsapp.domain.comments.repository.custom;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.kakaobase.snsapp.domain.members.entity.QMember.member;
import static com.kakaobase.snsapp.domain.comments.entity.QCommentLike.commentLike;

@Repository
@RequiredArgsConstructor
public class CommentLikeCustomRepositoryImpl implements CommentLikeCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberResponseDto.UserInfo> findMembersByCommentIdWithCursor(
            Long commentId,
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
                .from(commentLike)
                .join(commentLike.member, member)
                .where(
                        commentLike.comment.id.eq(commentId)
                                .and(lastMemberId != null ? member.id.lt(lastMemberId) : null)  // 커서 조건
                )
                .orderBy(member.id.desc())  // ID 내림차순 정렬
                .limit(limit)
                .fetch();
    }
}
