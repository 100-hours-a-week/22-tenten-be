package com.kakaobase.snsapp.domain.follow.repository.custom;

import static com.kakaobase.snsapp.domain.follow.entity.QFollow.follow;

import com.kakaobase.snsapp.domain.follow.dto.FollowCount;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.QMember;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class FollowCustomRepositoryImpl implements FollowCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Set<Long> findFollowingMemberIdsByFollowerAndTargets(Long followerId, List<Long> followingIds) {
        if (followingIds == null || followingIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> result = queryFactory
                .select(follow.followingUser.id)
                .from(follow)
                .where(
                        follow.followerUser.id.eq(followerId),
                        follow.followingUser.id.in(followingIds)
                )
                .fetch();

        return new HashSet<>(result);
    }

    @Override
    public List<MemberResponseDto.UserInfo> findFollowersByFollowingUserWithCursor(
            Long followingId, Integer limit, Long cursor) {

        QMember followerMember = new QMember("followerMember");

        return queryFactory
                .select(Projections.constructor(
                        MemberResponseDto.UserInfo.class,
                        followerMember.id,
                        followerMember.name,
                        followerMember.nickname,
                        followerMember.profileImgUrl
                ))
                .from(follow)
                .join(follow.followerUser, followerMember)
                .where(
                        follow.followingUser.id.eq(followingId),
                        cursorCondition(followerMember.id, cursor)
                )
                .orderBy(followerMember.id.asc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<MemberResponseDto.UserInfo> findFollowingsByFollowerUserWithCursor(
            Long followerId, Integer limit, Long cursor) {

        QMember followingMember = new QMember("followingMember");

        return queryFactory
                .select(Projections.constructor(
                        MemberResponseDto.UserInfo.class,
                        followingMember.id,
                        followingMember.name,
                        followingMember.nickname,
                        followingMember.profileImgUrl
                ))
                .from(follow)
                .join(follow.followingUser, followingMember)
                .where(
                        follow.followerUser.id.eq(followerId),
                        cursorCondition(followingMember.id, cursor)
                )
                .orderBy(followingMember.id.asc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Set<Long> findFollowingUserIdsByFollowerUserId(Long followerUserId) {
        List<Long> result = queryFactory
                .select(follow.followingUser.id)
                .from(follow)
                .where(follow.followerUser.id.eq(followerUserId))
                .fetch();

        return new HashSet<>(result);
    }

    @Override
    public List<FollowCount> findFollowingCounts() {
        return queryFactory
                .select(Projections.constructor(
                        FollowCount.class,
                        follow.followerUser.id,
                        follow.count()
                ))
                .from(follow)
                .groupBy(follow.followerUser.id)
                .fetch();
    }

    @Override
    public List<FollowCount> findFollowerCounts() {
        return queryFactory
                .select(Projections.constructor(
                        FollowCount.class,
                        follow.followingUser.id,
                        follow.count()
                ))
                .from(follow)
                .groupBy(follow.followingUser.id)
                .fetch();
    }

    @Override
    public Long countFollowersByFollowingUserId(Long followingUserId) {
        return queryFactory
                .select(follow.count())
                .from(follow)
                .where(follow.followingUser.id.eq(followingUserId))
                .fetchOne();
    }

    @Override
    public Long countFollowingsByFollowerUserId(Long followerUserId) {
        return queryFactory
                .select(follow.count())
                .from(follow)
                .where(follow.followerUser.id.eq(followerUserId))
                .fetchOne();
    }

    @Override
    public void cleanupAllFollowRelationships(Long memberId) {
        log.info("Starting cleanup of all follow relationships for member: {}", memberId);

        // 1. 해당 유저를 팔로우한 사람들의 팔로잉 카운트 감소
        Long decrementedFollowingCount = decrementFollowingCountForFollowersOf(memberId);
        log.debug("Decremented following count for {} members", decrementedFollowingCount);

        // 2. 해당 유저가 팔로우한 사람들의 팔로워 카운트 감소
        Long decrementedFollowerCount = decrementFollowerCountForFollowingsOf(memberId);
        log.debug("Decremented follower count for {} members", decrementedFollowerCount);

        // 3. 해당 유저가 다른 사람들을 팔로우한 관계 삭제
        Long deletedFollowingRelations = deleteByFollowerUserId(memberId);
        log.debug("Deleted {} following relationships", deletedFollowingRelations);

        // 4. 다른 사람들이 해당 유저를 팔로우한 관계 삭제
        Long deletedFollowerRelations = deleteByFollowingUserId(memberId);
        log.debug("Deleted {} follower relationships", deletedFollowerRelations);

        log.info("Completed cleanup of all follow relationships for member: {} " +
                        "(decremented: {} following, {} follower counts, deleted: {} following, {} follower relations)",
                memberId, decrementedFollowingCount, decrementedFollowerCount,
                deletedFollowingRelations, deletedFollowerRelations);
    }

    @Override
    public Long deleteByFollowingUserId(Long followingUserId) {
        log.info("Deleting all follow relationships where followingUserId = {}", followingUserId);

        return queryFactory
                .delete(follow)
                .where(follow.followingUser.id.eq(followingUserId))
                .execute();
    }

    @Override
    public Long deleteByFollowerUserId(Long followerUserId) {
        log.info("Deleting all follow relationships where followerUserId = {}", followerUserId);

        return queryFactory
                .delete(follow)
                .where(follow.followerUser.id.eq(followerUserId))
                .execute();
    }

    @Override
    public Long decrementFollowingCountForFollowersOf(Long followingUserId) {
        log.info("Decrementing followingCount for all followers of userId = {}", followingUserId);

        return queryFactory
                .update(QMember.member)
                .set(QMember.member.followingCount, QMember.member.followingCount.subtract(1))
                .where(
                        QMember.member.id.in(
                                queryFactory
                                        .select(follow.followerUser.id)
                                        .from(follow)
                                        .where(follow.followingUser.id.eq(followingUserId))
                        ),
                        QMember.member.followingCount.gt(0) // 0보다 큰 경우에만 감소
                )
                .execute();
    }

    @Override
    public Long decrementFollowerCountForFollowingsOf(Long followerUserId) {
        log.info("Decrementing followerCount for all followings of userId = {}", followerUserId);

        return queryFactory
                .update(QMember.member)
                .set(QMember.member.followerCount, QMember.member.followerCount.subtract(1))
                .where(
                        QMember.member.id.in(
                                queryFactory
                                        .select(follow.followingUser.id)
                                        .from(follow)
                                        .where(follow.followerUser.id.eq(followerUserId))
                        ),
                        QMember.member.followerCount.gt(0) // 0보다 큰 경우에만 감소
                )
                .execute();
    }

    // === 헬퍼 메서드 ===
    private BooleanExpression cursorCondition(com.querydsl.core.types.dsl.NumberPath<Long> idPath, Long cursor) {
        return cursor != null ? idPath.gt(cursor) : null;
    }
}
