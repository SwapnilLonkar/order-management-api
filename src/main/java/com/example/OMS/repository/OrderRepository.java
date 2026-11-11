package com.example.OMS.repository;

import com.example.OMS.entity.Order;
import com.example.OMS.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);

    Page<Order> findByPlacedBy(User placedBy, Pageable pageable);

    /** Orders containing at least one item whose product belongs to the given seller. */
    @Query("select distinct o from Order o join o.items i where i.product.seller = :seller order by o.id")
    List<Order> findByItemSeller(@Param("seller") User seller);
}