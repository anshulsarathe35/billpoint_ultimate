package com.billpoint.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.billpoint.backend.model.Bill;

public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByShop_Id(Long shopId);

    List<Bill> findByShop_IdAndCreatedAtBetween(Long shopId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    List<Bill> findByCustomer_Phone(String phone);

}