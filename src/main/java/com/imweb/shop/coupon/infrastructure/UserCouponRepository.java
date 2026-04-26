package com.imweb.shop.coupon.infrastructure;

import com.imweb.shop.coupon.domain.UserCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.id IN :ids ORDER BY uc.id ASC")
    List<UserCoupon> findAllByIdInForUpdate(@Param("ids") List<Long> ids);
}
