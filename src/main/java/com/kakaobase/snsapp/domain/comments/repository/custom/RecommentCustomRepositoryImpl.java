package com.kakaobase.snsapp.domain.comments.repository.custom;

import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.QRecomment;
import com.kakaobase.snsapp.domain.comments.entity.QRecommentLike;
import com.kakaobase.snsapp.domain.follow.entity.QFollow;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.QMember;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RecommentCustomRepositoryImpl implements RecommentCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<CommentResponseDto.RecommentInfo> findRecommentInfoById(Long recommentId, Long memberId) {
        QRecomment recomment = QRecomment.recomment;
        QMember member = QMember.member;
        QRecommentLike recommentLike = QRecommentLike.recommentLike;
        QFollow follow = QFollow.follow;

        CommentResponseDto.RecommentInfo result = queryFactory
                .select(Projections.constructor(CommentResponseDto.RecommentInfo.class,
                        recomment.id,

                        // 작성자 정보 (UserInfoWithFollowing)
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                recomment.member.id,
                                recomment.member.nickname,
                                recomment.member.profileImgUrl,
                                // 현재 사용자가 대댓글 작성자를 팔로우하는지 확인
                                follow.id.isNotNull()
                        ),

                        recomment.content,
                        recomment.createdAt,
                        recomment.likeCount,

                        // 본인 대댓글 여부
                        memberId != null ?
                                recomment.member.id.eq(memberId) :
                                Expressions.constant(false),

                        // 현재 사용자가 이 대댓글에 좋아요했는지 확인
                        recommentLike.id.isNotNull()
                ))
                .from(recomment)
                .join(recomment.member, member)

                // 현재 사용자의 좋아요만 LEFT JOIN
                .leftJoin(recommentLike).on(
                        recommentLike.recomment.eq(recomment)
                                .and(memberId != null ? recommentLike.id.memberId.eq(memberId) : null)
                )

                // 현재 사용자가 대댓글 작성자를 팔로우하는지 LEFT JOIN
                .leftJoin(follow).on(
                        memberId != null ? follow.followerUser.id.eq(memberId) : null,
                        follow.followingUser.eq(recomment.member)
                )

                .where(recomment.id.eq(recommentId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<CommentResponseDto.RecommentInfo> findRecommentInfoListWithCursor(
            Long commentId,
            Long cursor,
            int limit,
            Long memberId) {

        QRecomment recomment = QRecomment.recomment;
        QMember member = QMember.member;
        QRecommentLike recommentLike = QRecommentLike.recommentLike;
        QFollow follow = QFollow.follow;

        return queryFactory
                .select(Projections.constructor(CommentResponseDto.RecommentInfo.class,
                        recomment.id,

                        // 작성자 정보 (UserInfoWithFollowing)
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                recomment.member.id,
                                recomment.member.nickname,
                                recomment.member.profileImgUrl,
                                // 현재 사용자가 대댓글 작성자를 팔로우하는지 확인
                                follow.id.isNotNull()
                        ),

                        recomment.content,
                        recomment.createdAt,
                        recomment.likeCount,

                        // 본인 대댓글 여부
                        memberId != null ?
                                recomment.member.id.eq(memberId) :
                                Expressions.constant(false),

                        // 현재 사용자가 이 대댓글에 좋아요했는지 확인
                        recommentLike.id.isNotNull()
                ))
                .from(recomment)
                .join(recomment.member, member)

                // 현재 사용자의 좋아요만 LEFT JOIN
                .leftJoin(recommentLike).on(
                        recommentLike.recomment.eq(recomment)
                                .and(memberId != null ? recommentLike.id.memberId.eq(memberId) : null)
                )

                // 현재 사용자가 대댓글 작성자를 팔로우하는지 LEFT JOIN
                .leftJoin(follow).on(
                        memberId != null ? follow.followerUser.id.eq(memberId) : null,
                        follow.followingUser.eq(recomment.member)
                )

                .where(
                        recomment.comment.id.eq(commentId)
                                .and(cursor != null ? recomment.id.lt(cursor) : null)
                )
                .orderBy(recomment.createdAt.desc(), recomment.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<CommentResponseDto.RecommentInfo> findRecommentInfoListByMemberWithCursor(
            Long authorMemberId,
            Long cursor,
            int limit,
            Long currentMemberId) {

        QRecomment recomment = QRecomment.recomment;
        QMember member = QMember.member;
        QRecommentLike recommentLike = QRecommentLike.recommentLike;
        QFollow follow = QFollow.follow;

        return queryFactory
                .select(Projections.constructor(CommentResponseDto.RecommentInfo.class,
                        recomment.id,

                        // 작성자 정보 (authorMemberId로 고정)
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                recomment.member.id,
                                recomment.member.nickname,
                                recomment.member.profileImgUrl,
                                // 현재 사용자가 작성자를 팔로우하는지
                                follow.id.isNotNull()
                        ),

                        recomment.content,
                        recomment.createdAt,
                        recomment.likeCount,

                        // 본인 대댓글 여부 (authorMemberId == currentMemberId)
                        currentMemberId != null ?
                                recomment.member.id.eq(currentMemberId) :
                                Expressions.constant(false),

                        // 현재 사용자의 좋아요 여부
                        recommentLike.id.isNotNull()
                ))
                .from(recomment)
                .join(recomment.member, member)

                // 현재 사용자의 좋아요만 LEFT JOIN
                .leftJoin(recommentLike).on(
                        recommentLike.recomment.eq(recomment)
                                .and(currentMemberId != null ? recommentLike.id.memberId.eq(currentMemberId) : null)
                )

                // 현재 사용자가 작성자를 팔로우하는지 LEFT JOIN
                .leftJoin(follow).on(
                        currentMemberId != null ? follow.followerUser.id.eq(currentMemberId) : null,
                        follow.followingUser.id.eq(authorMemberId)
                )

                .where(
                        recomment.member.id.eq(authorMemberId)
                                .and(cursor != null ? recomment.id.lt(cursor) : null)
                )
                .orderBy(recomment.createdAt.desc(), recomment.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<CommentResponseDto.RecommentInfo> findReommentInfoListByLikedWithCursor(
            Long likedByMemberId,
            Long cursor,
            int limit,
            Long currentMemberId) {

        QRecomment recomment = QRecomment.recomment;
        QMember member = QMember.member;
        QRecommentLike recommentLike = QRecommentLike.recommentLike;
        QRecommentLike currentUserLike = new QRecommentLike("currentUserLike");
        QFollow follow = QFollow.follow;

        return queryFactory
                .select(Projections.constructor(CommentResponseDto.RecommentInfo.class,
                        recomment.id,

                        // 대댓글 작성자 정보
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                recomment.member.id,
                                recomment.member.nickname,
                                recomment.member.profileImgUrl,
                                // 현재 사용자가 대댓글 작성자를 팔로우하는지
                                follow.id.isNotNull()
                        ),

                        recomment.content,
                        recomment.createdAt,
                        recomment.likeCount,

                        // 본인 대댓글 여부
                        currentMemberId != null ?
                                recomment.member.id.eq(currentMemberId) :
                                Expressions.constant(false),

                        // 좋아요 여부 - likedByMemberId가 좋아요한 대댓글이므로 조건부 처리
                        likedByMemberId.equals(currentMemberId) ?
                                Expressions.constant(true) :
                                currentUserLike.id.isNotNull()
                ))
                .from(recomment)
                .join(recomment.member, member)

                // 핵심: likedByMemberId가 좋아요한 대댓글만 INNER JOIN
                .join(recommentLike).on(
                        recommentLike.recomment.eq(recomment)
                                .and(recommentLike.id.memberId.eq(likedByMemberId))
                )

                // 현재 사용자의 좋아요 여부 확인 (likedByMemberId와 다른 경우만)
                .leftJoin(currentUserLike).on(
                        currentUserLike.recomment.eq(recomment)
                                .and(currentMemberId != null && !currentMemberId.equals(likedByMemberId) ?
                                        currentUserLike.id.memberId.eq(currentMemberId) : null)
                )

                // 현재 사용자가 대댓글 작성자를 팔로우하는지
                .leftJoin(follow).on(
                        currentMemberId != null ? follow.followerUser.id.eq(currentMemberId) : null,
                        follow.followingUser.eq(recomment.member)
                )

                .where(
                        cursor != null ? recomment.id.lt(cursor) : null
                )
                .orderBy(recomment.createdAt.desc(), recomment.id.desc())
                .limit(limit)
                .fetch();
    }
}