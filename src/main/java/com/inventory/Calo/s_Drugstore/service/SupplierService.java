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

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAllByOrderByCompanyNameAsc();
    }

    public Optional<Supplier> getSupplierById(Long id) {
        return supplierRepository.findById(id);
    }

    public Optional<Supplier> getSupplierBySupplierId(String supplierId) {
        return supplierRepository.findBySupplierId(supplierId);
    }

    public List<Supplier> searchSuppliers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllSuppliers();
        }
        return supplierRepository.searchSuppliers(searchTerm.trim());
    }

    @Transactional
    public Supplier createSupplier(String companyName, String contactNumber, String physicalAddress,
                                   String contactPerson, String email, String productsSupplied) {

        String supplierId = generateNextSupplierId();

        Supplier supplier = new Supplier();
        supplier.setSupplierId(supplierId);
        supplier.setCompanyName(companyName);
        supplier.setContactNumber(contactNumber);
        supplier.setPhysicalAddress(physicalAddress);
        supplier.setContactPerson(contactPerson);
        supplier.setEmail(email);
        supplier.setProductsSupplied(productsSupplied);
        supplier.setStatus("Active");
        supplier.setDateAdded(LocalDate.now());
        supplier.setCreatedAt(LocalDate.now());
        supplier.setUpdatedAt(LocalDate.now());

        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getId() == null) {
            Optional<Supplier> existing = supplierRepository.findBySupplierId(supplier.getSupplierId());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Supplier ID already exists: " + supplier.getSupplierId());
            }
        }
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier updateSupplier(Supplier supplier) {
        if (supplier.getId() == null) {
            throw new IllegalArgumentException("Supplier ID cannot be null for update");
        }

        Optional<Supplier> existingSupplier = supplierRepository.findById(supplier.getId());
        if (existingSupplier.isEmpty()) {
            throw new IllegalArgumentException("Supplier not found with ID: " + supplier.getId());
        }

        supplier.setUpdatedAt(LocalDate.now());
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier updateSupplier(Long id, Supplier updatedSupplier) {
        Optional<Supplier> existingSupplier = supplierRepository.findById(id);

        if (existingSupplier.isPresent()) {
            Supplier supplier = existingSupplier.get();
            supplier.setCompanyName(updatedSupplier.getCompanyName());
            supplier.setContactNumber(updatedSupplier.getContactNumber());
            supplier.setPhysicalAddress(updatedSupplier.getPhysicalAddress());
            supplier.setContactPerson(updatedSupplier.getContactPerson());
            supplier.setEmail(updatedSupplier.getEmail());
            supplier.setProductsSupplied(updatedSupplier.getProductsSupplied());
            supplier.setStatus(updatedSupplier.getStatus());
            supplier.setUpdatedAt(LocalDate.now());
            return supplierRepository.save(supplier);
        }

        throw new IllegalArgumentException("Supplier not found with ID: " + id);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new IllegalArgumentException("Supplier not found with ID: " + id);
        }
        supplierRepository.deleteById(id);
    }

    public String generateNextSupplierId() {
        List<Supplier> allSuppliers = supplierRepository.findAll();

        if (allSuppliers.isEmpty()) {
            return "SUP001";
        }

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
                    // Ignore non-numeric suffixes
                }
            }
        }

        return String.format("SUP%03d", maxNumber + 1);
    }

    public List<Supplier> getSuppliersByStatus(String status) {
        return supplierRepository.findByStatus(status);
    }

    public long getTotalSupplierCount() {
        return supplierRepository.count();
    }

    public long countActiveSuppliers() {
        return supplierRepository.countByStatus("Active");
    }

    public long countSuppliersAddedThisMonth() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        return supplierRepository.countByDateAddedAfter(thirtyDaysAgo);
    }
}