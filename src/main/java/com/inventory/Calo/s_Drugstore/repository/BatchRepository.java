// Added to fix the duplication issue and to have the ability to see batches

package com.inventory.Calo.s_Drugstore.repository;

import com.inventory.Calo.s_Drugstore.entity.Batch;
import com.inventory.Calo.s_Drugstore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    // Find all batches for a product
    List<Batch> findByProduct(Product product);

    // Find batches by product ID
    @Query("SELECT b FROM Batch b WHERE b.product.id = :productId")
    List<Batch> findByProductId(@Param("productId") Long productId);

    // Find all batches for a product, ordered by expiration date (FEFO)
    @Query("SELECT b FROM Batch b WHERE b.product.id = :productId ORDER BY b.expirationDate ASC")
    List<Batch> findByProductOrderByExpirationDate(@Param("productId") Long productId);

    // Find batch by batch number
    Optional<Batch> findByBatchNumber(String batchNumber);

    // Find expiring batches
    @Query("SELECT b FROM Batch b WHERE b.expirationDate <= :date AND b.stock > 0")
    List<Batch> findExpiringBatches(@Param("date") LocalDate date);

    // Check if batch number exists
    boolean existsByBatchNumber(String batchNumber);

    // Count total batches for a product by product ID
    @Query("SELECT COUNT(b) FROM Batch b WHERE b.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);

    // Count total batches for a product
    long countByProduct(Product product);
}