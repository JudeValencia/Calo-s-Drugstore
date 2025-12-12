package com.inventory.Calo.s_Drugstore.service;

import com.inventory.Calo.s_Drugstore.entity.Supplier;
import com.inventory.Calo.s_Drugstore.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    // Get all suppliers
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAllByOrderByCompanyNameAsc();
    }

    // Get supplier by ID
    public Optional<Supplier> getSupplierById(Long id) {
        return supplierRepository.findById(id);
    }

    // Get supplier by supplier ID
    public Optional<Supplier> getSupplierBySupplierId(String supplierId) {
        return supplierRepository.findBySupplierId(supplierId);
    }

    // Search suppliers
    public List<Supplier> searchSuppliers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllSuppliers();
        }
        return supplierRepository.searchSuppliers(searchTerm.trim());
    }

    // Save supplier (create or update)
    @Transactional
    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getId() == null) {
            // New supplier - check if supplier ID already exists
            Optional<Supplier> existing = supplierRepository.findBySupplierId(supplier.getSupplierId());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Supplier ID already exists: " + supplier.getSupplierId());
            }
        }
        return supplierRepository.save(supplier);
    }

    // Update supplier
    @Transactional
    public Supplier updateSupplier(Long id, Supplier updatedSupplier) {
        Optional<Supplier> existingSupplier = supplierRepository.findById(id);

        if (existingSupplier.isPresent()) {
            Supplier supplier = existingSupplier.get();
            supplier.setCompanyName(updatedSupplier.getCompanyName());
            supplier.setContactPerson(updatedSupplier.getContactPerson());
            supplier.setMobileNumber(updatedSupplier.getMobileNumber());
            supplier.setEmail(updatedSupplier.getEmail());
            supplier.setPhysicalAddress(updatedSupplier.getPhysicalAddress());
            supplier.setUpdatedAt(LocalDate.now());
            return supplierRepository.save(supplier);
        }

        throw new IllegalArgumentException("Supplier not found with ID: " + id);
    }

    // Delete supplier
    @Transactional
    public void deleteSupplier(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new IllegalArgumentException("Supplier not found with ID: " + id);
        }
        supplierRepository.deleteById(id);
    }

    // Generate next supplier ID
    public String generateNextSupplierId() {
        List<Supplier> allSuppliers = supplierRepository.findAll();

        if (allSuppliers.isEmpty()) {
            return "SUP001";
        }

        // Find the highest supplier ID number
        int maxNumber = 0;
        for (Supplier supplier : allSuppliers) {
            String supplierId = supplier.getSupplierId();
            if (supplierId != null && supplierId.startsWith("SUP")) {
                try {
                    int number = Integer.parseInt(supplierId.substring(3));
                    if (number > maxNumber) {
                        maxNumber = number;
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid supplier IDs
                }
            }
        }

        return String.format("SUP%03d", maxNumber + 1);
    }

    // Get total count
    public long getTotalSupplierCount() {
        return supplierRepository.count();
    }
}