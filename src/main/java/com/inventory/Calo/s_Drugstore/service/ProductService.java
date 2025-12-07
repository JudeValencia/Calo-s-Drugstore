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
import java.time.temporal.ChronoUnit;
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
                //existing.setDescription(product.getDescription());
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
            //product.setDescription(updatedProduct.getDescription());
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
        // Check batches instead of products for accurate expiry count
        return getExpiringBatchesCount(30);
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


    @Transactional
    public Product saveProductWithBatch(Product product, Integer batchStock,
                                        LocalDate batchExpiry, BigDecimal batchPrice,
                                        String batchSupplier) {
        Product targetProduct;

        // Check if this is an existing product (has an ID) or new product
        if (product.getId() != null) {
            // EXISTING PRODUCT - Just add a new batch
            targetProduct = productRepository.findById(product.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + product.getId()));

            System.out.println("✅ Adding batch to EXISTING product: " + targetProduct.getBrandName() + " (ID: " + targetProduct.getId() + ")");
        } else {
            // NEW PRODUCT - Check if one with same Brand Name already exists
            List<Product> existingByName = productRepository.searchByNameOrId(product.getBrandName());
            Product existingMatch = existingByName.stream()
                    .filter(p -> p.getBrandName().equalsIgnoreCase(product.getBrandName()))
                    .findFirst()
                    .orElse(null);

            if (existingMatch != null) {
                // Product with same name exists - add batch to it instead of creating duplicate
                targetProduct = existingMatch;
                System.out.println("✅ Found existing product by name: " + targetProduct.getBrandName() + " - Adding batch instead of creating duplicate");
            } else {
                // Completely new product - save it first
                targetProduct = productRepository.save(product);
                System.out.println("✅ Created NEW product: " + targetProduct.getBrandName() + " (ID: " + targetProduct.getId() + ")");
            }
        }

        // ✅ NEW: Check if product has stock but no batches (legacy stock)
        long existingBatchCount = batchRepository.countByProductId(targetProduct.getId());
        if (existingBatchCount == 0 && targetProduct.getStock() > 0) {
            // Create a legacy batch for existing stock
            System.out.println("⚠️ Found legacy stock: " + targetProduct.getStock() + " units with no batch records");

            Batch legacyBatch = new Batch();
            legacyBatch.setBatchNumber("LEGACY-" + targetProduct.getMedicineId() + "-" + System.currentTimeMillis());
            legacyBatch.setProduct(targetProduct);
            legacyBatch.setStock(targetProduct.getStock());
            legacyBatch.setExpirationDate(targetProduct.getExpirationDate() != null ?
                    targetProduct.getExpirationDate() :
                    LocalDate.now().plusYears(2)); // Default 2 years if no expiry
            legacyBatch.setPrice(targetProduct.getPrice());
            legacyBatch.setSupplier(targetProduct.getSupplier() != null ?
                    targetProduct.getSupplier() : "Unknown");
            legacyBatch.setDateReceived(LocalDate.now());

            batchRepository.save(legacyBatch);
            System.out.println("✅ Created legacy batch: " + legacyBatch.getBatchNumber() +
                    " with " + legacyBatch.getStock() + " units");
        }

        // Create the new batch
        Batch newBatch = new Batch();
        newBatch.setBatchNumber(generateBatchNumber(targetProduct.getMedicineId()));
        newBatch.setProduct(targetProduct);
        newBatch.setStock(batchStock);
        newBatch.setExpirationDate(batchExpiry);
        newBatch.setPrice(batchPrice);
        newBatch.setSupplier(batchSupplier);
        newBatch.setDateReceived(LocalDate.now());

        batchRepository.save(newBatch);
        System.out.println("✅ Created batch: " + newBatch.getBatchNumber() + " with " + batchStock + " units, expiry: " + batchExpiry);

        // Update product's total stock by summing all batches
        int totalStock = batchRepository.findByProductId(targetProduct.getId()).stream()
                .mapToInt(Batch::getStock)
                .sum();
        targetProduct.setStock(totalStock);
        targetProduct.setUpdatedAt(LocalDate.now());

        Product savedProduct = productRepository.save(targetProduct);
        System.out.println("✅ Updated product total stock to: " + totalStock + " units");

        // Clear cache after modification
        clearBatchCache();

        return savedProduct;
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

    // Deduct stock using FEFO (First Expiry First Out)
    // Returns a JSON string with batch deduction history for restoration
    @Transactional
    public String deductStockFromBatches(Long productId, int quantityToDeduct) {
        // Get all batches for this product, ordered by expiration date (FEFO)
        List<Batch> batches = batchRepository.findByProductOrderByExpirationDate(productId);

        if (batches.isEmpty()) {
            throw new RuntimeException("No batches available for product ID: " + productId);
        }

        int remainingToDeduct = quantityToDeduct;
        StringBuilder batchInfoJson = new StringBuilder("[");
        boolean first = true;

        for (Batch batch : batches) {
            if (remainingToDeduct <= 0) {
                break;
            }

            int batchStock = batch.getStock();

            if (batchStock > 0) {
                int deductFromThisBatch = Math.min(remainingToDeduct, batchStock);

                // Deduct from this batch
                batch.setStock(batchStock - deductFromThisBatch);
                batchRepository.save(batch);

                // Track this deduction for restoration
                if (!first) batchInfoJson.append(",");
                batchInfoJson.append("{\"batchId\":")
                        .append(batch.getId())
                        .append(",\"batchNumber\":\"")
                        .append(batch.getBatchNumber())
                        .append("\",\"quantity\":")
                        .append(deductFromThisBatch)
                        .append(",\"expiryDate\":\"")
                        .append(batch.getExpirationDate())
                        .append("\"}");
                first = false;

                remainingToDeduct -= deductFromThisBatch;

                System.out.println("✅ FEFO: Deducted " + deductFromThisBatch + " from batch " +
                        batch.getBatchNumber() + " (Remaining in batch: " + batch.getStock() + ")");
            }
        }

        if (remainingToDeduct > 0) {
            throw new RuntimeException("Insufficient batch stock. Could not fulfill " +
                    remainingToDeduct + " units from batches.");
        }

        batchInfoJson.append("]");

        // Update product's total stock
        int totalStock = batchRepository.findByProductId(productId).stream()
                .mapToInt(Batch::getStock)
                .sum();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStock(totalStock);
        productRepository.save(product);

        System.out.println("✅ Product total stock updated to: " + totalStock);
        System.out.println("📝 Batch deduction info: " + batchInfoJson.toString());

        // Clear cache
        clearBatchCache();
        
        return batchInfoJson.toString();
    }
    @Transactional(readOnly = true)
    public List<Product> getProductsWithExpiringBatches(int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        System.out.println("=== DEBUG: getProductsWithExpiringBatches ===");
        System.out.println("Checking from: " + today + " to " + endDate);

        // 1. Get expiring batches (from today to future days)
        List<Batch> expiringBatches = batchRepository.findExpiringBatches(today, endDate);
        System.out.println("Found " + expiringBatches.size() + " expiring batches");

        List<Product> productsFromBatches = expiringBatches.stream()
                .map(batch -> {
                    Product product = batch.getProduct();
                    product.getBrandName(); // Initialize proxy
                    return product;
                })
                .distinct()
                .filter(product -> {
                    // Double-check: only include if earliest batch is actually expiring (not expired)
                    LocalDate earliestExpiry = getEarliestExpiryDate(product);
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), earliestExpiry);
                    return daysLeft >= 0 && daysLeft <= days;
                })
                .toList();

        System.out.println("Products from batches: " + productsFromBatches.size());

        // 2. Get ALL expired/expiring legacy products (no batches)
        List<Product> legacyExpiringProducts = productRepository.findExpiringProducts(endDate).stream()
                .filter(p -> {
                    long batchCount = batchRepository.countByProductId(p.getId());
                    return batchCount == 0 && p.getExpirationDate() != null;
                })
                .toList();

        System.out.println("Legacy products without batches: " + legacyExpiringProducts.size());

        // 3. Combine both
        List<Product> allProducts = new java.util.ArrayList<>(productsFromBatches);
        for (Product legacy : legacyExpiringProducts) {
            if (!allProducts.contains(legacy)) {
                allProducts.add(legacy);
            }
        }

        // Sort by expiration date
        allProducts.sort((p1, p2) -> {
            LocalDate date1 = getEarliestExpiryDate(p1);
            LocalDate date2 = getEarliestExpiryDate(p2);
            return date1.compareTo(date2);
        });

        System.out.println("Total unique expiring products: " + allProducts.size());
        System.out.println("===========================================");

        return allProducts;
    }

    private LocalDate getEarliestExpiryDate(Product product) {
        List<Batch> batches = batchRepository.findByProductOrderByExpirationDate(product.getId());
        if (!batches.isEmpty()) {
            return batches.get(0).getExpirationDate();
        }
        return product.getExpirationDate();
    }
    public long getExpiringBatchesCount(int days) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(days);
        List<Batch> expiringBatches = batchRepository.findExpiringBatches(today, targetDate);
        return expiringBatches.stream()
                .map(Batch::getProduct)
                .map(Product::getId)
                .distinct()
                .count();
    }
}