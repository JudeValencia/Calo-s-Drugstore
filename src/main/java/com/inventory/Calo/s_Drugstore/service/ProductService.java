package com.inventory.Calo.s_Drugstore.service;

import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // Get all products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Get product by ID
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // Get product by medicine ID
    public Optional<Product> getProductByMedicineId(String medicineId) {
        return productRepository.findByMedicineId(medicineId);
    }

    // Search products
    public List<Product> searchProducts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.searchByNameOrId(searchTerm.trim());
    }

    // Filter products
    public List<Product> filterProducts(String searchTerm, String supplier, String category) {
        // Convert "All Suppliers" or "All Categories" to null
        String supplierFilter = (supplier == null || supplier.equals("All Suppliers")) ? null : supplier;
        String categoryFilter = (category == null || category.equals("All Categories")) ? null : category;
        String searchFilter = (searchTerm == null || searchTerm.trim().isEmpty()) ? null : searchTerm.trim();

        return productRepository.findByFilters(searchFilter, supplierFilter, categoryFilter);
    }

    // Get products by supplier
    public List<Product> getProductsBySupplier(String supplier) {
        return productRepository.findBySupplier(supplier);
    }

    // Get products by category
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    // Get low stock products
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    // Get expiring products (within 30 days)
    public List<Product> getExpiringProducts() {
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        return productRepository.findExpiringProducts(thirtyDaysFromNow);
    }

    // Get all suppliers
    public List<String> getAllSuppliers() {
        return productRepository.findAllSuppliers();
    }

    // Get all categories
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    // Save product (add or update based on Medicine ID)
    @Transactional
    public Product saveProduct(Product product) {
        if (product.getId() == null) {
            // New product - check if medicine ID already exists
            Optional<Product> existingProduct = productRepository.findByMedicineId(product.getMedicineId());

            if (existingProduct.isPresent()) {
                // Medicine ID exists - update the existing product instead of creating new one
                Product existing = existingProduct.get();

                // Update all fields from the new product data
                existing.setBrandName(product.getBrandName());
                existing.setGenericName(product.getGenericName());
                existing.setStock(product.getStock());
                existing.setPrice(product.getPrice());
                existing.setExpirationDate(product.getExpirationDate());
                existing.setSupplier(product.getSupplier());
                existing.setCategory(product.getCategory());
                existing.setDescription(product.getDescription());
                existing.setMinStockLevel(product.getMinStockLevel());
                existing.setBatchNumber(product.getBatchNumber());
                existing.setPrescriptionRequired(product.getPrescriptionRequired());
                existing.setDosageForm(product.getDosageForm());
                existing.setDosageStrength(product.getDosageStrength());
                existing.setManufacturer(product.getManufacturer());
                existing.setUnitOfMeasure(product.getUnitOfMeasure());
                existing.setUpdatedAt(LocalDate.now());

                return productRepository.save(existing);
            }
        }

        // Either it's a completely new product, or it's an update with existing ID
        return productRepository.save(product);
    }

    // Update product
    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        Optional<Product> existingProduct = productRepository.findById(id);

        if (existingProduct.isPresent()) {
            Product product = existingProduct.get();

            // Update fields
            product.setBrandName(updatedProduct.getBrandName());
            product.setStock(updatedProduct.getStock());
            product.setPrice(updatedProduct.getPrice());
            product.setExpirationDate(updatedProduct.getExpirationDate());
            product.setSupplier(updatedProduct.getSupplier());
            product.setCategory(updatedProduct.getCategory());
            product.setDescription(updatedProduct.getDescription());
            product.setMinStockLevel(updatedProduct.getMinStockLevel());
            product.setUpdatedAt(LocalDate.now());

            return productRepository.save(product);
        }

        throw new IllegalArgumentException("Product not found with ID: " + id);
    }

    // Update stock
    @Transactional
    public Product updateStock(Long id, Integer newStock) {
        Optional<Product> existingProduct = productRepository.findById(id);

        if (existingProduct.isPresent()) {
            Product product = existingProduct.get();
            product.setStock(newStock);
            product.setUpdatedAt(LocalDate.now());
            return productRepository.save(product);
        }

        throw new IllegalArgumentException("Product not found with ID: " + id);
    }

    // Delete product
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);
    }

    // Get statistics
    public long getTotalProductCount() {
        return productRepository.count();
    }

    public long getLowStockCount() {
        return productRepository.countLowStockProducts();
    }

    public long getExpiringProductsCount() {
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        return productRepository.countExpiringProducts(thirtyDaysFromNow);
    }

    // Generate next medicine ID
    public String generateNextMedicineId() {
        List<Product> allProducts = productRepository.findAll();

        if (allProducts.isEmpty()) {
            return "MED001";
        }

        // Find the highest medicine ID number
        int maxNumber = 0;
        for (Product product : allProducts) {
            String medicineId = product.getMedicineId();
            if (medicineId != null && medicineId.startsWith("MED")) {
                try {
                    int number = Integer.parseInt(medicineId.substring(3));
                    if (number > maxNumber) {
                        maxNumber = number;
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid medicine IDs
                }
            }
        }

        return String.format("MED%03d", maxNumber + 1);

    }
}