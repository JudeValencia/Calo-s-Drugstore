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

    // Find today's sales - EXCLUDING VOIDED
    @Query("SELECT s FROM Sale s WHERE s.saleDate >= :startOfDay AND s.saleDate < :endOfDay AND (s.voided IS NULL OR s.voided = FALSE) ORDER BY s.saleDate DESC")
    List<Sale> findTodaysSales(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    // Find ALL today's sales including voided
    @Query("SELECT s FROM Sale s WHERE s.saleDate >= :startOfDay AND s.saleDate < :endOfDay ORDER BY s.saleDate DESC")
    List<Sale> findAllTodaysSalesIncludingVoided(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    // Find only voided transactions
    @Query("SELECT s FROM Sale s WHERE s.voided = TRUE ORDER BY s.voidDate DESC")
    List<Sale> findVoidedTransactions();

    // Get the latest transaction ID for generating new IDs
    @Query("SELECT s FROM Sale s ORDER BY s.id DESC")
    List<Sale> findLatestSales();

    // Find sales between dates - EXCLUDING VOIDED
    @Query("SELECT s FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate AND (s.voided IS NULL OR s.voided = FALSE) ORDER BY s.saleDate DESC")
    List<Sale> findBySaleDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find ALL sales between dates including voided
    @Query("SELECT s FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate ORDER BY s.saleDate DESC")
    List<Sale> findAllBySaleDateBetweenIncludingVoided(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}