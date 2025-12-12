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

    // Find non-expired batches for a product, ordered by expiration date (FEFO)
    @Query("SELECT b FROM Batch b WHERE b.product.id = :productId AND b.expirationDate > :today ORDER BY b.expirationDate ASC")
    List<Batch> findNonExpiredBatchesByProductId(@Param("productId") Long productId, @Param("today") LocalDate today);

    // Find batch by batch number
    Optional<Batch> findByBatchNumber(String batchNumber);

    // Find expiring batches
    // Find batches that expired in last 30 days OR will expire in next 30 days
    @Query("SELECT b FROM Batch b WHERE b.expirationDate >= :startDate AND b.expirationDate <= :endDate AND b.stock > 0 ORDER BY b.expirationDate ASC")
    List<Batch> findExpiringBatches(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Find products that have expiration dates but NO batches
    @Query("SELECT p FROM Product p WHERE p.expirationDate >= :startDate AND p.expirationDate <= :endDate " +
            "AND p.stock > 0 AND NOT EXISTS (SELECT b FROM Batch b WHERE b.product = p)")
    List<Product> findProductsExpiringWithoutBatches(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    // Check if batch number exists
    boolean existsByBatchNumber(String batchNumber);

    // Count total batches for a product by product ID
    @Query("SELECT COUNT(b) FROM Batch b WHERE b.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);

    // Count total batches for a product
    long countByProduct(Product product);
}