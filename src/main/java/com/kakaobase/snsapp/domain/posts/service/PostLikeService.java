package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.members.converter.MemberConverter;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.service.MemberService;
import com.kakaobase.snsapp.domain.posts.converter.PostConverter;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.entity.PostLike;
import com.kakaobase.snsapp.domain.posts.exception.PostErrorCode;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostLikeRepository;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        // 게시글 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));

        // 이미 좋아요한 경우 확인
        if (postLikeRepository.existsByMemberIdAndPostId(memberId, postId)) {
            throw new PostException(PostErrorCode.ALREADY_LIKED);
        }

        Member proxyMember = em.getReference(Member.class, memberId);

        // 좋아요 엔티티 생성 및 저장
        PostLike postLike = new PostLike(proxyMember, post);
        postLikeRepository.save(postLike);

        // 게시글 좋아요 수 증가
        post.increaseLikeCount();

        log.info("게시글 좋아요 추가 완료: 게시글 ID={}, 회원 ID={}", postId, memberId);
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
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));

        // 좋아요 존재 여부 확인
        PostLike postLike = postLikeRepository.findByMemberIdAndPostId(memberId, postId)
                .orElseThrow(() -> new PostException(PostErrorCode.ALREADY_UNLIKED));

        // 좋아요 삭제
        postLikeRepository.delete(postLike);

        // 게시글 좋아요 수 감소
        post.decreaseLikeCount();

        log.info("게시글 좋아요 취소 완료: 게시글 ID={}, 회원 ID={}", postId, memberId);
    }

    /**
     * 회원이 게시글에 좋아요했는지 확인합니다.
     *
     * @param postId 게시글 ID
     * @param memberId 회원 ID
     * @return 좋아요 여부
     */
    public boolean isLikedByMember(Long postId, Long memberId) {
        return postLikeRepository.existsByMemberIdAndPostId(memberId, postId);
    }

    /**
     * 회원이 좋아요한 게시글 ID 목록을 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 좋아요한 게시글 ID 목록
     */
    public List<Long> findLikedPostIdsByMember(Long memberId) {
        return postLikeRepository.findPostIdsByMemberId(memberId);
    }

    /**
     * 게시글 목록 중 회원이 좋아요한 게시글 ID 목록을 조회합니다.
     */
    public List<Long> findLikedPostIdsByMember(Long memberId, List<Post> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        return postLikeRepository.findPostIdsByMemberIdAndPostIdIn(memberId, postIds);
    }

    @Transactional(readOnly = true)
    public List<MemberResponseDto.UserInfo> getLikedMembers(Long postId, int limit, Long cursor) {

        if(!postRepository.existsById(postId)){
            throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND);
        }

        List<Member> members = postLikeRepository.findMembersByPostIdWithCursor(postId, cursor, limit);

        List<MemberResponseDto.UserInfo> result = memberConverter.convertToUserInfoList(members);

        return result;
    }

    /**
     * 게시글 삭제 시 연관된 좋아요를 일괄 삭제합니다.
     *
     * @param postId 게시글 ID
     */
    @Transactional
    public void deleteAllByPostId(Long postId) {
        postLikeRepository.deleteByPostId(postId);
        log.info("게시글 관련 좋아요 일괄 삭제 완료: 게시글 ID={}", postId);
    }
}