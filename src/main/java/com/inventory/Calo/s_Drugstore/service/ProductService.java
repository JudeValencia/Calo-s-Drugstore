package com.inventory.Calo.s_Drugstore.service;

import com.inventory.Calo.s_Drugstore.entity.Batch;
import com.inventory.Calo.s_Drugstore.repository.BatchRepository;
import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BatchRepository batchRepository;

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

    // Save product (add or update)
    @Transactional
    public Product saveProduct(Product product) {
        if (product.getId() == null) {
            // New product - check if medicine ID already exists
            if (productRepository.existsByMedicineId(product.getMedicineId())) {
                throw new IllegalArgumentException("Medicine ID already exists: " + product.getMedicineId());
            }
        }
        return productRepository.save(product);
    }

    // Update product
    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        Optional<Product> existingProduct = productRepository.findById(id);

        if (existingProduct.isPresent()) {
            Product product = existingProduct.get();

            // Update fields
            product.setName(updatedProduct.getName());
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

    //New added methods below for batch support
    // Generate batch number
    public String generateBatchNumber(String medicineId) {
        String prefix = "BATCH-" + medicineId + "-";
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = batchRepository.count() + 1;
        return prefix + timestamp + "-" + String.format("%03d", count);
    }

    // Save product with batch
    @Transactional
    public Product saveProductWithBatch(Product product, Integer batchStock,
                                        LocalDate batchExpiry, BigDecimal batchPrice,
                                        String batchSupplier) {
        // Check if product with same name already exists
        List<Product> existingProducts = productRepository.searchByNameOrId(product.getName());
        Product existingProduct = existingProducts.stream()
                .filter(p -> p.getName().equalsIgnoreCase(product.getName()))
                .findFirst()
                .orElse(null);

        if (existingProduct != null) {
            // Product exists, just add a new batch
            Batch newBatch = new Batch();
            newBatch.setBatchNumber(generateBatchNumber(existingProduct.getMedicineId()));
            newBatch.setProduct(existingProduct);
            newBatch.setStock(batchStock);
            newBatch.setExpirationDate(batchExpiry);
            newBatch.setPrice(batchPrice);
            newBatch.setSupplier(batchSupplier);
            newBatch.setDateReceived(LocalDate.now());

            batchRepository.save(newBatch);

            // Update product's total stock by summing all batches using product ID
            int totalStock = batchRepository.findByProductId(existingProduct.getId()).stream()
                    .mapToInt(Batch::getStock)
                    .sum();
            existingProduct.setStock(totalStock);
            existingProduct.setUpdatedAt(LocalDate.now());

            return productRepository.save(existingProduct);
        } else {
            // New product, create product first
            Product savedProduct = productRepository.save(product);

            // Then create first batch
            Batch newBatch = new Batch();
            newBatch.setBatchNumber(generateBatchNumber(savedProduct.getMedicineId()));
            newBatch.setProduct(savedProduct);
            newBatch.setStock(batchStock);
            newBatch.setExpirationDate(batchExpiry);
            newBatch.setPrice(batchPrice);
            newBatch.setSupplier(batchSupplier);
            newBatch.setDateReceived(LocalDate.now());

            batchRepository.save(newBatch);

            // Update product's stock
            savedProduct.setStock(batchStock);
            return productRepository.save(savedProduct);
        }
    }

    // Get all batches for a product
    @Transactional(readOnly = true)
    public List<Batch> getBatchesForProduct(Product product) {
        // Use cache during table rendering to improve performance
        return batchCache.computeIfAbsent(product.getId(),
                id -> batchRepository.findByProductOrderByExpirationDate(id));
    }

    //Get batch by ID
    public Optional<Batch> getBatchById(Long id) {
        return batchRepository.findById(id);
    }

    //Get batch by number
    public Optional<Batch> getBatchByNumber(String batchNumber) {
        return batchRepository.findByBatchNumber(batchNumber);
    }

    //Update batch stock
    @Transactional
    public void updateBatchStock(Long batchId, Integer newStock) {
        Optional<Batch> batchOpt = batchRepository.findById(batchId);
        if (batchOpt.isPresent()) {
            Batch batch = batchOpt.get();
            batch.setStock(newStock);
            batchRepository.save(batch);

            // Update product's total stock using product ID
            Product product = batch.getProduct();
            int totalStock = batchRepository.findByProductId(product.getId()).stream()
                    .mapToInt(Batch::getStock)
                    .sum();
            product.setStock(totalStock);
            productRepository.save(product);
        }
    }

    //Delete a batch
    @Transactional
    public void deleteBatch(Long batchId) {
        Optional<Batch> batchOpt = batchRepository.findById(batchId);
        if (batchOpt.isPresent()) {
            Batch batch = batchOpt.get();
            Product product = batch.getProduct();

            batchRepository.delete(batch);

            // Update product's total stock using product ID
            int totalStock = batchRepository.findByProductId(product.getId()).stream()
                    .mapToInt(Batch::getStock)
                    .sum();
            product.setStock(totalStock);
            productRepository.save(product);
        }
    }

    //Get expiring batches within specified days
    public List<Batch> getExpiringBatches(int days) {
        LocalDate targetDate = LocalDate.now().plusDays(days);
        return batchRepository.findExpiringBatches(targetDate);
    }

    //Count total number of batches for a product
    public long countBatchesForProduct(Product product) {
        return batchRepository.countByProductId(product.getId());
    }

    // Cache for batch lookups during table rendering
    private Map<Long, List<Batch>> batchCache = new ConcurrentHashMap<>();

    // Call this after any batch modification to clear cache
    public void clearBatchCache() {
        batchCache.clear();
    }
}