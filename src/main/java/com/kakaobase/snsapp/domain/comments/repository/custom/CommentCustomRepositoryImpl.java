package com.kakaobase.snsapp.domain.comments.repository.custom;

import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.QComment;
import com.kakaobase.snsapp.domain.comments.entity.QCommentLike;
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
public class CommentCustomRepositoryImpl implements CommentCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<CommentResponseDto.CommentInfo> findCommentInfoById(Long commentId, Long memberId) {
        QComment comment = QComment.comment;
        QMember member = QMember.member;
        QCommentLike commentLike = QCommentLike.commentLike;
        QFollow follow = QFollow.follow;

        CommentResponseDto.CommentInfo result = queryFactory
                .select(Projections.constructor(CommentResponseDto.CommentInfo.class,
                        comment.id,
                        comment.post.id,

                        // 작성자 정보 (UserInfoWithFollowing)
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                comment.member.id,
                                comment.member.nickname,
                                comment.member.profileImgUrl,
                                // 현재 사용자가 댓글 작성자를 팔로우하는지 확인
                                follow.id.isNotNull()
                        ),

                        comment.content,
                        comment.createdAt,
                        comment.likeCount,
                        comment.recommentCount,

                        // 본인 댓글 여부
                        memberId != null ?
                                comment.member.id.eq(memberId) :
                                Expressions.constant(false),

                        // 현재 사용자가 이 댓글에 좋아요했는지 확인
                        commentLike.id.isNotNull()
                ))
                .from(comment)
                .join(comment.member, member)

                // 현재 사용자의 좋아요만 LEFT JOIN
                .leftJoin(commentLike).on(
                        commentLike.comment.eq(comment)
                                .and(memberId != null ? commentLike.id.memberId.eq(memberId) : null)
                )

                // 현재 사용자가 댓글 작성자를 팔로우하는지 LEFT JOIN
                .leftJoin(follow).on(
                        memberId != null ? follow.followerUser.id.eq(memberId) : null,
                        follow.followingUser.eq(comment.member)
                )

                .where(comment.id.eq(commentId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<CommentResponseDto.CommentInfo> findCommentInfoListWithCursor(
            Long postId,
            Long cursor,
            int limit,
            Long memberId) {

        QComment comment = QComment.comment;
        QMember member = QMember.member;
        QCommentLike commentLike = QCommentLike.commentLike;
        QFollow follow = QFollow.follow;

        return queryFactory
                .select(Projections.constructor(CommentResponseDto.CommentInfo.class,
                        comment.id,
                        comment.post.id,

                        // 작성자 정보 (UserInfoWithFollowing)
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                comment.member.id,
                                comment.member.nickname,
                                comment.member.profileImgUrl,
                                // 현재 사용자가 댓글 작성자를 팔로우하는지 확인
                                follow.id.isNotNull()
                        ),

                        comment.content,
                        comment.createdAt,
                        comment.likeCount,
                        comment.recommentCount,

                        // 본인 댓글 여부
                        memberId != null ?
                                comment.member.id.eq(memberId) :
                                Expressions.constant(false),

                        // 현재 사용자가 이 댓글에 좋아요했는지 확인
                        commentLike.id.isNotNull()
                ))
                .from(comment)
                .join(comment.member, member)

                // 현재 사용자의 좋아요만 LEFT JOIN
                .leftJoin(commentLike).on(
                        commentLike.comment.eq(comment)
                                .and(memberId != null ? commentLike.id.memberId.eq(memberId) : null)
                )

                // 현재 사용자가 댓글 작성자를 팔로우하는지 LEFT JOIN
                .leftJoin(follow).on(
                        memberId != null ? follow.followerUser.id.eq(memberId) : null,
                        follow.followingUser.eq(comment.member)
                )

                .where(
                        comment.post.id.eq(postId)
                                .and(cursor != null ? comment.id.lt(cursor) : null)
                )
                .orderBy(comment.createdAt.asc(), comment.id.asc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<CommentResponseDto.CommentInfo> findCommentInfoByMemberWithCursor(
            Long authorMemberId,
            Long cursor,
            int limit,
            Long currentMemberId) {

        QComment comment = QComment.comment;
        QMember member = QMember.member;
        QCommentLike commentLike = QCommentLike.commentLike;
        QFollow follow = QFollow.follow;

        return queryFactory
                .select(Projections.constructor(CommentResponseDto.CommentInfo.class,
                        comment.id,
                        comment.post.id,

                        // 작성자 정보 (authorMemberId로 고정)
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                comment.member.id,
                                comment.member.nickname,
                                comment.member.profileImgUrl,
                                // 현재 사용자가 작성자를 팔로우하는지
                                follow.id.isNotNull()
                        ),

                        comment.content,
                        comment.createdAt,
                        comment.likeCount,
                        comment.recommentCount,

                        // 본인 댓글 여부 (authorMemberId == currentMemberId)
                        currentMemberId != null ?
                                comment.member.id.eq(currentMemberId) :
                                Expressions.constant(false),

                        // 현재 사용자의 좋아요 여부
                        commentLike.id.isNotNull()
                ))
                .from(comment)
                .join(comment.member, member)

                // 현재 사용자의 좋아요만 LEFT JOIN
                .leftJoin(commentLike).on(
                        commentLike.comment.eq(comment)
                                .and(currentMemberId != null ? commentLike.id.memberId.eq(currentMemberId) : null)
                )

                // 현재 사용자가 작성자를 팔로우하는지 LEFT JOIN
                .leftJoin(follow).on(
                        currentMemberId != null ? follow.followerUser.id.eq(currentMemberId) : null,
                        follow.followingUser.id.eq(authorMemberId)
                )

                .where(
                        comment.member.id.eq(authorMemberId)
                                .and(cursor != null ? comment.id.lt(cursor) : null)
                )
                .orderBy(comment.createdAt.asc(), comment.id.asc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<CommentResponseDto.CommentInfo> findCommentInfoListByLikedWithCursor(
            Long likedByMemberId,
            Long cursor,
            int limit,
            Long currentMemberId) {

        QComment comment = QComment.comment;
        QMember member = QMember.member;
        QCommentLike commentLike = QCommentLike.commentLike;
        QCommentLike currentUserLike = new QCommentLike("currentUserLike");
        QFollow follow = QFollow.follow;

        return queryFactory
                .select(Projections.constructor(CommentResponseDto.CommentInfo.class,
                        comment.id,
                        comment.post.id,

                        // 댓글 작성자 정보
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                comment.member.id,
                                comment.member.nickname,
                                comment.member.profileImgUrl,
                                // 현재 사용자가 댓글 작성자를 팔로우하는지
                                follow.id.isNotNull()
                        ),

                        comment.content,
                        comment.createdAt,
                        comment.likeCount,
                        comment.recommentCount,

                        // 본인 댓글 여부
                        currentMemberId != null ?
                                comment.member.id.eq(currentMemberId) :
                                Expressions.constant(false),

                        // 좋아요 여부 - likedByMemberId가 좋아요한 댓글이므로 조건부 처리
                        likedByMemberId.equals(currentMemberId) ?
                                Expressions.constant(true) :
                                currentUserLike.id.isNotNull()
                ))
                .from(comment)
                .join(comment.member, member)

                // 핵심: likedByMemberId가 좋아요한 댓글만 INNER JOIN
                .join(commentLike).on(
                        commentLike.comment.eq(comment)
                                .and(commentLike.id.memberId.eq(likedByMemberId))
                )

                // 현재 사용자의 좋아요 여부 확인 (likedByMemberId와 다른 경우만)
                .leftJoin(currentUserLike).on(
                        currentUserLike.comment.eq(comment)
                                .and(currentMemberId != null && !currentMemberId.equals(likedByMemberId) ?
                                        currentUserLike.id.memberId.eq(currentMemberId) : null)
                )

                // 현재 사용자가 댓글 작성자를 팔로우하는지
                .leftJoin(follow).on(
                        currentMemberId != null ? follow.followerUser.id.eq(currentMemberId) : null,
                        follow.followingUser.eq(comment.member)
                )

                .where(
                        cursor != null ? comment.id.lt(cursor) : null
                )
                .orderBy(comment.createdAt.asc(), comment.id.asc())
                .limit(limit)
                .fetch();
    }
}
