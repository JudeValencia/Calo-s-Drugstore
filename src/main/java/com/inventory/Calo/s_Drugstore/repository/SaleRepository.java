package com.inventory.Calo.s_Drugstore.repository;

import com.inventory.Calo.s_Drugstore.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    // Find today's sales - WITH PARAMETERS
    @Query("SELECT s FROM Sale s WHERE s.saleDate >= :startOfDay AND s.saleDate < :endOfDay ORDER BY s.saleDate DESC")
    List<Sale> findTodaysSales(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    // Get the latest transaction ID for generating new IDs
    @Query("SELECT s FROM Sale s ORDER BY s.id DESC")
    List<Sale> findLatestSales();

    List<Sale> findBySaleDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}