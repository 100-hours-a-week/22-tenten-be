package com.kakaobase.snsapp.domain.members.repository;

import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 회원 정보에 접근하기 위한 레포지토리 인터페이스입니다.
 * 인덱싱 전략에 맞춰 최적화된 쿼리 메서드들을 제공합니다.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일로 활성 회원을 조회합니다. (인덱스: idx_email_not_deleted 사용)
     *
     * @param email 회원 이메일
     * @return 이메일에 해당하는 회원, 없으면 Optional.empty()
     */
    Optional<Member> findByEmail(String email);

    /**
     * 이메일로 활성 회원 존재 여부를 확인합니다. (인덱스: idx_email_not_deleted 사용)
     *
     * @param email 확인할 이메일
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByEmail(String email);

    /**
     * 닉네임으로 활성 회원 존재 여부를 확인합니다. (인덱스: idx_nickname_not_deleted 사용)
     *
     * @param nickname 확인할 닉네임
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByNickname(String nickname);

    /**
     * 기수별 활성 회원 목록을 조회합니다.
     *
     * @param className 기수명
     * @return 해당 기수의 회원 목록
     */
    List<Member> findByClassName(Member.ClassName className);

    /**
     * 회원 ID 목록으로 활성 회원 목록을 조회합니다.
     *
     * @param ids 회원 ID 목록
     * @return 조회된 회원 목록
     */
    List<Member> findByIdIn(List<Long> ids);

    // MemberRepository.java 수정
    @Query("SELECT m FROM Member m WHERE " +
            "(:name IS NOT NULL AND :name != '' AND m.name LIKE %:name%) " +
            "AND m.deletedAt IS NULL")
    List<Member> searchByName(@Param("name") String name);

    @Query("SELECT m FROM Member m WHERE " +
            "(:nickname IS NOT NULL AND :nickname != '' AND m.nickname LIKE %:nickname%) " +
            "AND m.deletedAt IS NULL")
    List<Member> searchByNickname(@Param("nickname") String nickname);

    @Query("SELECT m FROM Member m WHERE " +
            "(:nickname IS NOT NULL AND :nickname != '' AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :nickname, '%'))) " +
            "ORDER BY m.nickname ASC LIMIT :limit")
    List<Member> findByNicknameContainingLimit(@Param("nickname") String nickname, @Param("limit") int limit);

    /**
     * 특정 회원의 프로필 정보와 팔로우 통계를 조회합니다.
     * 필요한 컬럼만 선택하여 성능을 최적화합니다.
     *
     * @param id 회원 ID
     * @return 회원 프로필 정보
     */
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findProfileById(@Param("id") Long id);

    /**
     * 여러 회원 ID로 회원 목록을 조회합니다.
     * 게시글 목록 조회 시 여러 작성자 정보를 한 번에 가져오는 데 사용됩니다.
     *
     * @param ids 조회할 회원 ID 목록
     * @return 조회된 회원 엔티티 목록
     */
    List<Member> findAllByIdIn(List<Long> ids);

    /**
     * 정확한 닉네임으로 회원을 조회합니다.
     * 특정 닉네임을 가진 회원의 정보가 필요할 때 사용됩니다.
     *
     * @param nickname 조회할 정확한 닉네임
     * @return 조회된 회원 엔티티 (Optional)
     */
    Optional<Member> findByNickname(String nickname);

    /**
     * 여러 닉네임으로 회원 목록을 조회합니다.
     * 여러 닉네임에 해당하는 회원 정보를 한 번에 가져오는 데 사용됩니다.
     * 주로 'who_liked' 목록 처리 시 닉네임 목록을 ID로 변환할 때 활용됩니다.
     *
     * @param nicknames 조회할 닉네임 목록
     * @return 조회된 회원 엔티티 목록
     */
    List<Member> findAllByNicknameIn(List<String> nicknames);
}