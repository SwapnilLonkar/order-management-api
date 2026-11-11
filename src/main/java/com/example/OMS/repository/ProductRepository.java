package com.example.OMS.repository;

import com.example.OMS.entity.Product;
import com.example.OMS.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findBySeller(User seller, Pageable pageable);
}
