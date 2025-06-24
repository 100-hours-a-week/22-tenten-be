package com.kakaobase.snsapp.domain.posts.repository.custom;

import com.kakaobase.snsapp.domain.follow.entity.QFollow;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.QMember;
import com.kakaobase.snsapp.domain.posts.dto.PostResponseDto;
import com.kakaobase.snsapp.domain.posts.util.BoardType;
import com.kakaobase.snsapp.domain.posts.entity.QPost;
import com.kakaobase.snsapp.domain.posts.entity.QPostImage;
import com.kakaobase.snsapp.domain.posts.entity.QPostLike;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostCustomRepositoryImpl implements PostCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<PostResponseDto.PostDetails> findPostDetailById(Long postId, Long memberId) {

        QPost post = QPost.post;
        QMember member = QMember.member;
        QPostImage postImage = QPostImage.postImage;
        QPostLike postLike = QPostLike.postLike;
        QFollow follow = QFollow.follow;

        PostResponseDto.PostDetails result = queryFactory
                .select(Projections.constructor(PostResponseDto.PostDetails.class,
                        // 기본 Post 필드들
                        post.id,

                        // 이중 DTO: UserInfoWithFollowing 생성
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                post.member.id,
                                post.member.nickname,
                                post.member.profileImgUrl,
                                // ✅ 현재 사용자가 게시글 작성자를 팔로우하는지 확인
                                follow.id.isNotNull()
                        ),

                        post.content,

                        // ✅ 첫 번째 이미지 URL
                        postImage.imgUrl,

                        post.youtubeUrl,
                        post.youtubeSummary,
                        post.createdAt,
                        post.likeCount,
                        post.commentCount,

                        // ✅ 본인 게시글 여부
                        memberId != null ?
                                post.member.id.eq(memberId) :
                                Expressions.constant(false),

                        // ✅ 현재 사용자가 이 게시글에 좋아요했는지 확인
                        postLike.id.isNotNull()
                ))
                .from(post)
                .join(post.member, member)

                // ✅ 첫 번째 이미지만 LEFT JOIN
                .leftJoin(postImage).on(
                        postImage.post.eq(post)
                                .and(postImage.sortIndex.eq(0))  // 첫 번째 이미지만
                )

                // ✅ 현재 사용자의 좋아요만 LEFT JOIN
                .leftJoin(postLike).on(
                        postLike.post.eq(post)
                                .and(memberId != null ? postLike.id.memberId.eq(memberId) : null)
                )

                // ✅ 현재 사용자가 게시글 작성자를 팔로우하는지 LEFT JOIN
                .leftJoin(follow).on(
                        memberId != null ? follow.followerUser.id.eq(memberId) : null,  // 현재 사용자가
                        follow.followingUser.eq(post.member)  // 게시글 작성자를 팔로우
                )

                .where(post.id.eq(postId))  // ✅ 단일 조회 조건
                .fetchOne();  // ✅ 단일 결과 조회

        return Optional.ofNullable(result);
    }

    @Override
    public List<PostResponseDto.PostDetails> findByBoardTypeWithCursor(
            BoardType boardType,
            Long cursor,
            int limit,
            Long memberId) {

        QPost post = QPost.post;
        QMember member = QMember.member;
        QPostImage postImage = QPostImage.postImage;
        QPostLike postLike = QPostLike.postLike;
        QFollow follow = QFollow.follow;

        return queryFactory
                .select(Projections.constructor(PostResponseDto.PostDetails.class,
                        // 기본 Post 필드들
                        post.id,

                        // 이중 DTO: UserInfoWithFollowing 생성
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                post.member.id,
                                post.member.nickname,
                                post.member.profileImgUrl,
                                // ✅ 현재 사용자가 게시글 작성자를 팔로우하는지 확인
                                follow.id.isNotNull()
                        ),

                        post.content,

                        // ✅ 첫 번째 이미지 URL (PostImage 엔티티 필드명 확인 필요)
                        postImage.imgUrl,  // PostImage.imgUrl 필드 사용

                        post.youtubeUrl,
                        post.youtubeSummary,
                        post.createdAt,
                        post.likeCount,
                        post.commentCount,

                        // ✅ 본인 게시글 여부
                        memberId != null ?
                                post.member.id.eq(memberId) :
                                Expressions.constant(false),

                        // ✅ 현재 사용자가 이 게시글에 좋아요했는지 확인
                        postLike.id.isNotNull()
                ))
                .from(post)
                .join(post.member, member)

                // ✅ 첫 번째 이미지만 LEFT JOIN
                .leftJoin(postImage).on(
                        postImage.post.eq(post)
                                .and(postImage.sortIndex.eq(0))  // 첫 번째 이미지만
                )

                // ✅ 현재 사용자의 좋아요만 LEFT JOIN
                .leftJoin(postLike).on(
                        postLike.post.eq(post)
                                .and(memberId != null ? postLike.id.memberId.eq(memberId) : null)
                )

                // ✅ 현재 사용자가 게시글 작성자를 팔로우하는지 LEFT JOIN
                .leftJoin(follow).on(
                        memberId != null ? follow.followerUser.id.eq(memberId) : null,  // 현재 사용자가
                        follow.followingUser.eq(post.member)  // 게시글 작성자를 팔로우
                )

                .where(
                        post.boardType.eq(boardType)
                                .and(cursor != null ? post.id.lt(cursor) : null)
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<PostResponseDto.PostDetails> findByMemberWithCursor(
            Long authorMemberId,  // 게시글 작성자 ID
            Long cursor,
            int limit,
            Long currentMemberId) {  // 현재 로그인한 사용자 ID

        QPost post = QPost.post;
        QMember member = QMember.member;
        QPostImage postImage = QPostImage.postImage;
        QPostLike postLike = QPostLike.postLike;
        QFollow follow = QFollow.follow;

        return queryFactory
                .select(Projections.constructor(PostResponseDto.PostDetails.class,
                        post.id,

                        // 작성자 정보 (authorMemberId로 고정)
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                post.member.id,
                                post.member.nickname,
                                post.member.profileImgUrl,
                                // 현재 사용자가 작성자를 팔로우하는지
                                follow.id.isNotNull()
                        ),

                        post.content,
                        postImage.imgUrl,
                        post.youtubeUrl,
                        post.youtubeSummary,
                        post.createdAt,
                        post.likeCount,
                        post.commentCount,

                        // 본인 게시글 여부 (authorMemberId == currentMemberId)
                        currentMemberId != null ?
                                post.member.id.eq(currentMemberId) :
                                Expressions.constant(false),

                        // 현재 사용자의 좋아요 여부
                        postLike.id.isNotNull()
                ))
                .from(post)
                .join(post.member, member)

                // 첫 번째 이미지만 LEFT JOIN
                .leftJoin(postImage).on(
                        postImage.post.eq(post)
                                .and(postImage.sortIndex.eq(0))
                )

                // 현재 사용자의 좋아요만 LEFT JOIN
                .leftJoin(postLike).on(
                        postLike.post.eq(post)
                                .and(currentMemberId != null ? postLike.id.memberId.eq(currentMemberId) : null)
                )

                // 현재 사용자가 작성자를 팔로우하는지 LEFT JOIN
                .leftJoin(follow).on(
                        currentMemberId != null ? follow.followerUser.id.eq(currentMemberId) : null,
                        follow.followingUser.id.eq(authorMemberId)  // 작성자 ID로 고정
                )

                .where(
                        post.member.id.eq(authorMemberId)  // 특정 작성자의 게시글만
                                .and(cursor != null ? post.id.lt(cursor) : null)
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(limit)
                .fetch();
    }

    // 2. 특정 사용자가 좋아요한 게시글 조회 (기존 findLikedPostsByMemberIdWithCursor 대체)
    @Override
    public List<PostResponseDto.PostDetails> findLikedPostsWithCursor(
            Long likedByMemberId,  // 좋아요한 사용자 ID
            Long cursor,
            int limit,
            Long currentMemberId) {  // 현재 로그인한 사용자 ID

        QPost post = QPost.post;
        QMember member = QMember.member;
        QPostImage postImage = QPostImage.postImage;
        QPostLike postLike = QPostLike.postLike;
        QPostLike currentUserLike = new QPostLike("currentUserLike");  // 별칭 사용
        QFollow follow = QFollow.follow;

        return queryFactory
                .select(Projections.constructor(PostResponseDto.PostDetails.class,
                        post.id,

                        // 게시글 작성자 정보
                        Projections.constructor(MemberResponseDto.UserInfoWithFollowing.class,
                                post.member.id,
                                post.member.nickname,
                                post.member.profileImgUrl,
                                // 현재 사용자가 게시글 작성자를 팔로우하는지
                                follow.id.isNotNull()
                        ),

                        post.content,
                        postImage.imgUrl,
                        post.youtubeUrl,
                        post.youtubeSummary,
                        post.createdAt,
                        post.likeCount,
                        post.commentCount,

                        // 본인 게시글 여부
                        currentMemberId != null ?
                                post.member.id.eq(currentMemberId) :
                                Expressions.constant(false),

                        // 좋아요 여부 - likedByMemberId가 좋아요한 게시글이므로 조건부 처리
                        likedByMemberId.equals(currentMemberId) ?
                                Expressions.constant(true) :  // 자신이 좋아요한 게시글 목록이면 모두 true
                                currentUserLike.id.isNotNull()  // 다른 사람이 좋아요한 게시글이면 현재 사용자 좋아요 여부
                ))
                .from(post)
                .join(post.member, member)

                // 핵심: likedByMemberId가 좋아요한 게시글만 INNER JOIN
                .join(postLike).on(
                        postLike.post.eq(post)
                                .and(postLike.id.memberId.eq(likedByMemberId))
                )

                // 첫 번째 이미지만 LEFT JOIN
                .leftJoin(postImage).on(
                        postImage.post.eq(post)
                                .and(postImage.sortIndex.eq(0))
                )

                // 현재 사용자의 좋아요 여부 확인 (likedByMemberId와 다른 경우만)
                .leftJoin(currentUserLike).on(
                        currentUserLike.post.eq(post)
                                .and(currentMemberId != null && !currentMemberId.equals(likedByMemberId) ?
                                        currentUserLike.id.memberId.eq(currentMemberId) : null)
                )

                // 현재 사용자가 게시글 작성자를 팔로우하는지
                .leftJoin(follow).on(
                        currentMemberId != null ? follow.followerUser.id.eq(currentMemberId) : null,
                        follow.followingUser.eq(post.member)
                )

                .where(
                        cursor != null ? post.id.lt(cursor) : null
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(limit)
                .fetch();
    }
}
