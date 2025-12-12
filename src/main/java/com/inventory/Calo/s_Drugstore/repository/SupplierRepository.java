package com.inventory.Calo.s_Drugstore.repository;

import com.inventory.Calo.s_Drugstore.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    // Find by supplier ID
    Optional<Supplier> findBySupplierId(String supplierId);

    // Find by company name
    Optional<Supplier> findByCompanyName(String companyName);

    // Search suppliers by name, contact person, or supplier ID
    @Query("SELECT s FROM Supplier s WHERE " +
            "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.supplierId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Supplier> searchSuppliers(@Param("searchTerm") String searchTerm);

    // Get all suppliers ordered by company name
    List<Supplier> findAllByOrderByCompanyNameAsc();
}
