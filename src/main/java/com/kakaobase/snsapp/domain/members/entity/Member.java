package com.kakaobase.snsapp.domain.members.entity;

import com.kakaobase.snsapp.domain.comments.entity.CommentLike;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.entity.RecommentLike;
import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.entity.PostLike;
import com.kakaobase.snsapp.global.common.entity.BaseSoftDeletableEntity;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(
        name = "members",
        indexes = {
                @Index(name = "idx_email_not_deleted", columnList = "email, deleted_at"),
                @Index(name = "idx_nickname_not_deleted", columnList = "nickname, deleted_at"),
                @Index(name = "idx_role_not_deleted", columnList = "role, deleted_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@BatchSize(size = 50)
@Where(clause = "deleted_at IS NULL")
public class Member extends BaseSoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'USER'")
    private Role role = Role.USER;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(name = "class_name", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ClassName className;

    @Column(name = "profile_img_url", length = 512)
    private String profileImgUrl;

    @Column(name = "github_url", length = 255)
    private String githubUrl;

    @Column(name = "is_banned")
    @ColumnDefault("false")
    private Boolean isBanned = false;

    @Column(name = "following_count", nullable = false)
    @ColumnDefault("0")
    private Long followingCount = 0L;

    @Column(name = "follower_count", nullable = false)
    @ColumnDefault("0")
    private Long followerCount = 0L;

    @Builder
    public Member(String email, String name, String nickname, String password, ClassName className, String githubUrl) {
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.password = password;
        this.className = className;
        this.githubUrl = githubUrl;
    }

    /**
     * 회원 역할을 정의하는 열거형입니다.
     */
    public enum Role {
        USER,   // 일반 사용자
        ADMIN,  // 관리자
        BOT     // 봇
    }

    /**
     * 카카오 테크 부트캠프 기수를 정의하는 열거형입니다.
     */
    public enum ClassName {
        PANGYO_1,
        PANGYO_2,
        JEJU_1,
        JEJU_2,
        JEJU_3,
        ALL
    }


    /**
     * 회원 프로필 이미지를 업데이트합니다.
     */
    public void updateProfile(String profileImgUrl) {
        this.profileImgUrl = profileImgUrl;
    }

    /**
     * GitHub URL을 업데이트합니다.
     */
    public void updateGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    /**
     * 비밀번호를 업데이트합니다.
     */
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    /**
     * 회원 역할을 업데이트합니다.
     */
    public void updateRole(Role role) {
        this.role = role;
    }

    /**
     * 회원 밴 상태를 변경합니다.
     */
    public void updateBanStatus(boolean isBanned) {
        this.isBanned = isBanned;
    }


    public void updateFollowingCount(Long followingCount) {
        this.followingCount = followingCount;
    }

    /**
     * 팔로잉 카운트를 증가시킵니다.
     */
    public void incrementFollowingCount() {
        this.followingCount++;
    }

    /**
     * 팔로잉 카운트를 감소시킵니다.
     */
    public void decrementFollowingCount() {
        if (this.followingCount > 0) {
            this.followingCount--;
        }
    }

    public void updateFollowerCount(Long followerCount) {
        this.followerCount = followerCount;
    }

    /**
     * 팔로워 카운트를 증가시킵니다.
     */
    public void incrementFollowerCount() {
        this.followerCount++;
    }

    /**
     * 팔로워 카운트를 감소시킵니다.
     */
    public void decrementFollowerCount() {
        if (this.followerCount > 0) {
            this.followerCount--;
        }
    }

    /**
     * 회원 역할을 문자열로 반환합니다.
     * JWT 토큰에 반환하는 타입 호환성을 위해 별도로 선언됩니다.
     *
     * @return 역할 이름(String)
     */
    public String getRole() {
        return this.role.name();
    }

    /**
     * 회원 기수를 문자열로 반환합니다.
     * JWT 토큰에 저장하기 위한 용도로 사용됩니다.
     *
     * @return 기수 이름(String)
     */
    public String getClassName() {
        return this.className.name();
    }

    /**
     * 계정이 활성화되어 있는지 확인합니다.
     * (삭제되지 않았고, 밴 상태가 아닌 경우)
     */
    public boolean isEnabled() {
        return !isDeleted() && !isBanned;
    }

    // === 연관관계 매핑 ===

    // 작성한 게시글들 (1:N)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<Post> posts = new HashSet<>();

    // 작성한 댓글들 (1:N)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<Comment> comments = new HashSet<>();

    // 작성한 대댓글들 (1:N)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<Recomment> recomments = new HashSet<>();

    // 팔로우 관계 - 나를 팔로우하는 사람들 (1:N)
    @OneToMany(mappedBy = "followerUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<Follow> followings = new HashSet<>();

    // 팔로워 관계 - 내가 팔로잉하는 사람들 (1:N)
    @OneToMany(mappedBy = "followingUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<Follow> followers = new HashSet<>();

    // 좋아요한 게시글들 (1:N)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<PostLike> postLikes = new HashSet<>();

    // 좋아요한 댓글들 (1:N)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<CommentLike> commentLikes = new HashSet<>();

    // 좋아요한 대댓글들 (1:N)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<RecommentLike> recommentLikes = new HashSet<>();
}