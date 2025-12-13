package com.inventory.Calo.s_Drugstore.repository;

import com.inventory.Calo.s_Drugstore.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    // Find by supplier ID
    Optional<Supplier> findBySupplierId(String supplierId);

    // Search suppliers by name, contact person, or supplier ID
    @Query("SELECT s FROM Supplier s WHERE " +
            "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.supplierId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.contactNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Supplier> searchSuppliers(@Param("searchTerm") String searchTerm);

    // Get all suppliers ordered by company name
    List<Supplier> findAllByOrderByCompanyNameAsc();

    // Filter by status
    List<Supplier> findByStatus(String status);

    // Count by status
    long countByStatus(String status);

    // Count suppliers added after specific date
    long countByDateAddedAfter(LocalDate date);

    Optional<Supplier> findByCompanyName(String supplierName);
}