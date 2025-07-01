package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.members.converter.MemberConverter;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.notification.service.NotificationService;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.entity.PostLike;
import com.kakaobase.snsapp.domain.posts.exception.PostErrorCode;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostLikeRepository;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.domain.posts.service.cache.PostCacheService;
import com.kakaobase.snsapp.global.common.redis.error.CacheException;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글 좋아요 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final EntityManager em;
    private final PostCacheService postCacheService;
    private final NotificationService notifService;
    private final MemberConverter memberConverter;

    /**
     * 게시글에 좋아요를 추가합니다.
     *
     * @param postId 게시글 ID
     * @param memberId 회원 ID
     * @throws PostException 게시글이 없거나 이미 좋아요한 경우
     */
    @Transactional
    public void addLike(Long postId, Long memberId) {
        if(!postRepository.existsById(postId)) {
            throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId");
        }

        // 이미 좋아요한 경우 확인
        if (postLikeRepository.existsByMemberIdAndPostId(memberId, postId)) {
            throw new PostException(PostErrorCode.ALREADY_LIKED);
        }

        Post proxyPost = em.getReference(Post.class, postId);
        Member proxyMember = em.getReference(Member.class, memberId);

        //게시글 좋아요 수 캐싱 처리
        try{
            postCacheService.incrementLikeCount(postId);
        } catch (CacheException e){
            log.error(e.getMessage());
            proxyPost.increaseLikeCount();
        }

        // 좋아요 엔티티 생성 및 저장
        PostLike postLike = new PostLike(proxyMember, proxyPost);
        postLikeRepository.save(postLike);
        log.info("게시글 좋아요 추가 완료: 게시글 ID={}, 회원 ID={}", postId, memberId);

        if(!proxyPost.getMember().getId().equals(memberId)) {
            MemberResponseDto.UserInfo userInfo = memberConverter.toUserInfo(proxyMember);
           notifService.sendPostLikeCreatedNotification(proxyPost.getMember().getId(), proxyPost.getId(),null, userInfo, postId);
        }
    }

    /**
     * 게시글 좋아요를 취소합니다.
     *
     * @param postId 게시글 ID
     * @param memberId 회원 ID
     * @throws PostException 게시글이 없거나 좋아요하지 않은 경우
     */
    @Transactional
    public void removeLike(Long postId, Long memberId) {
        // 게시글 존재 여부 확인
        if(!postRepository.existsById(postId)) {
            throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId");
        }

        // 좋아요 존재 여부 확인
        PostLike postLike = postLikeRepository.findByMemberIdAndPostId(memberId, postId)
                .orElseThrow(() -> new PostException(PostErrorCode.ALREADY_UNLIKED));

        try{
            postCacheService.decrementLikeCount(postId);
        } catch (CacheException e){
            log.error(e.getMessage());
            Post proxyPost = em.getReference(Post.class, postId);
            proxyPost.decreaseLikeCount();
        }

        // 좋아요 삭제
        postLikeRepository.delete(postLike);
        log.info("게시글 좋아요 취소 완료: 게시글 ID={}, 회원 ID={}", postId, memberId);
    }


    //특정 게시물에 좋아요를 누른 유저 정보 조회
    @Transactional(readOnly = true)
    public List<MemberResponseDto.UserInfo> getLikedMembers(Long postId, int limit, Long cursor) {

        if(!postRepository.existsById(postId)){
            throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND);
        }

        return postLikeRepository.findMembersByPostIdWithCursor(postId, cursor, limit);
    }
}