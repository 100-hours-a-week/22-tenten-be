package com.kakaobase.snsapp.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 소프트 삭제(Soft Delete) 기능을 제공하는 엔티티 클래스입니다.
 * 생성 시간과 수정 시간 외에 삭제 시간 정보를 추가로 관리합니다.
 *
 * 물리적 삭제 대신 논리적 삭제를 수행하는 엔티티(예: member, post, comment 등)에서 사용합니다.
 *
 * 이 클래스를 상속받는 엔티티는 다음 어노테이션을 추가해야 합니다:
 * {@code @SQLDelete(sql = "UPDATE 테이블명 SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")}
 *
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@FilterDef(name = "deletedFilter", defaultCondition = "deleted_at IS NULL")
@Filter(name = "deletedFilter")
public abstract class BaseSoftDeletableEntity extends BaseUpdateTimeEntity {

    /**
     * 엔티티가 삭제된 시간입니다.
     * null이 아닌 경우 엔티티가 삭제된 것으로 간주합니다.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 엔티티를 소프트 삭제 처리합니다.
     * 삭제 시간을 현재 시간으로 설정합니다.
     *
     * 이 메서드는 @SQLDelete 어노테이션이 적용되지 않는 상황에서
     * 프로그래밍 방식으로 엔티티를 삭제할 때 사용합니다.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 엔티티의 삭제 여부를 확인합니다.
     *
     * @return 엔티티가 삭제되었으면 true, 그렇지 않으면 false
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}