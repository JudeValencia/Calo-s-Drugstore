package com.inventory.Calo.s_Drugstore.repository;

import com.inventory.Calo.s_Drugstore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find by medicine ID
    Optional<Product> findByMedicineId(String medicineId);

    // Search by name or medicine ID
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.medicineId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Product> searchByNameOrId(@Param("searchTerm") String searchTerm);

    // Find by supplier
    List<Product> findBySupplier(String supplier);

    // Find by category
    List<Product> findByCategory(String category);

    // Find low stock products
    @Query("SELECT p FROM Product p WHERE p.stock <= p.minStockLevel")
    List<Product> findLowStockProducts();

    // Find expiring products (within specified days)
    @Query("SELECT p FROM Product p WHERE p.expirationDate <= :date")
    List<Product> findExpiringProducts(@Param("date") LocalDate date);

    // Get all suppliers (distinct)
    @Query("SELECT DISTINCT p.supplier FROM Product p WHERE p.supplier IS NOT NULL ORDER BY p.supplier")
    List<String> findAllSuppliers();

    // Get all categories (distinct)
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findAllCategories();

    // Count total products
    long count();

    // Count low stock products
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock <= p.minStockLevel")
    long countLowStockProducts();

    // Count expiring products (within 30 days)
    @Query("SELECT COUNT(p) FROM Product p WHERE p.expirationDate <= :date")
    long countExpiringProducts(@Param("date") LocalDate date);

    // Check if medicine ID exists
    boolean existsByMedicineId(String medicineId);

    // Advanced search with multiple filters
    @Query("SELECT p FROM Product p WHERE " +
            "(:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.medicineId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:supplier IS NULL OR p.supplier = :supplier) AND " +
            "(:category IS NULL OR p.category = :category)")
    List<Product> findByFilters(
            @Param("searchTerm") String searchTerm,
            @Param("supplier") String supplier,
            @Param("category") String category
    );
}