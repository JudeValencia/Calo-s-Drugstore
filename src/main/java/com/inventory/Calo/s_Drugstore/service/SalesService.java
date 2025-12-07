package com.inventory.Calo.s_Drugstore.service;

import com.inventory.Calo.s_Drugstore.entity.*;
import com.inventory.Calo.s_Drugstore.repository.BatchRepository;
import com.inventory.Calo.s_Drugstore.repository.ProductRepository;
import com.inventory.Calo.s_Drugstore.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public Sale completeSale(List<SaleItem> cartItems, User user) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Create new sale
        Sale sale = new Sale();
        sale.setTransactionId(generateTransactionId());
        sale.setSaleDate(LocalDateTime.now());
        sale.setUserId(user.getId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItems = 0;

        // Process each item
        for (SaleItem item : cartItems) {
            // Get fresh product from database
            Optional<Product> productOpt = productService.getProductById(item.getProduct().getId());

            if (!productOpt.isPresent()) {
                throw new RuntimeException("Product not found: " + item.getMedicineName());
            }

            Product product = productOpt.get();

            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for " + product.getBrandName() +
                        ". Available: " + product.getStock() + ", Requested: " + item.getQuantity());
            }

            // ✅ NEW: Deduct from batches using FEFO and get batch deduction history
            String batchInfo = productService.deductStockFromBatches(product.getId(), item.getQuantity());

            // Create new sale item (don't reuse cart item)
            SaleItem saleItem = new SaleItem();
            saleItem.setMedicineId(product.getMedicineId());
            saleItem.setMedicineName(product.getBrandName());
            saleItem.setQuantity(item.getQuantity());
            saleItem.setUnitPrice(item.getUnitPrice());
            saleItem.setSubtotal(item.getSubtotal());
            saleItem.setBatchInfo(batchInfo);  // Store batch deduction history

            // Add to sale
            sale.addItem(saleItem);

            totalAmount = totalAmount.add(item.getSubtotal());
            totalItems += item.getQuantity();
        }

        sale.setTotalAmount(totalAmount);
        sale.setTotalItems(totalItems);

        // Save sale (will cascade to items)
        Sale savedSale = saleRepository.save(sale);

        System.out.println("✅ Sale saved: " + savedSale.getTransactionId() +
                " with " + savedSale.getItems().size() + " items");

        return savedSale;
    }


    public Map<String, Object> getTodaysSummary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        List<Sale> todaysSales = saleRepository.findTodaysSales(startOfDay, endOfDay);

        Map<String, Object> summary = new HashMap<>();

        int totalTransactions = todaysSales.size();
        int totalItemsSold = 0;
        BigDecimal totalSales = BigDecimal.ZERO;

        for (Sale sale : todaysSales) {
            totalItemsSold += sale.getTotalItems();
            totalSales = totalSales.add(sale.getTotalAmount());
        }

        summary.put("totalTransactions", totalTransactions);
        summary.put("totalItemsSold", totalItemsSold);
        summary.put("totalSales", totalSales);

        return summary;
    }

    // THIS METHOD EXISTS - MAKE SURE IT'S IN YOUR FILE
    public List<Sale> getTodaysTransactions() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        return saleRepository.findTodaysSales(startOfDay, endOfDay);
    }

    private String generateTransactionId() {
        List<Sale> latestSales = saleRepository.findLatestSales();

        if (latestSales.isEmpty()) {
            return "TXN001";
        }

        String lastId = latestSales.get(0).getTransactionId();
        int number = Integer.parseInt(lastId.substring(3)) + 1;
        return String.format("TXN%03d", number);
    }

    public List<Sale> getSalesBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.findBySaleDateBetween(startDate, endDate);
    }

    // Get ALL transactions including voided (for viewing in reports with filter)
    public List<Sale> getAllSalesBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.findAllBySaleDateBetweenIncludingVoided(startDate, endDate);
    }

    // Get only voided transactions
    public List<Sale> getVoidedTransactions() {
        return saleRepository.findVoidedTransactions();
    }

    @Transactional
    public Sale updateTransaction(Sale sale) {
        // Validate sale has ID
        if (sale.getId() == null) {
            throw new RuntimeException("Cannot update transaction without ID");
        }

        // Recalculate totals based on items
        BigDecimal newTotal = BigDecimal.ZERO;
        int newTotalItems = 0;

        for (SaleItem item : sale.getItems()) {
            newTotal = newTotal.add(item.getSubtotal());
            newTotalItems += item.getQuantity();
        }

        sale.setTotalAmount(newTotal);
        sale.setTotalItems(newTotalItems);

        // Update the sale (cascade will update items)
        return saleRepository.save(sale);
    }

    @Transactional
    public void deleteTransaction(Long saleId) {
        Optional<Sale> saleOpt = saleRepository.findById(saleId);

        if (!saleOpt.isPresent()) {
            throw new RuntimeException("Transaction not found");
        }

        Sale sale = saleOpt.get();

        // Restore inventory for all items
        for (SaleItem item : sale.getItems()) {
            // Find product by medicine ID
            List<Product> allProducts = productService.getAllProducts();
            Optional<Product> productOpt = allProducts.stream()
                    .filter(p -> p.getMedicineId().equals(item.getMedicineId()))
                    .findFirst();

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                String batchInfo = item.getBatchInfo();

                if (batchInfo != null && !batchInfo.isEmpty() && !batchInfo.equals("[]")) {
                    // ✅ PERFECT RESTORATION: Use stored batch info for exact restoration
                    System.out.println("🔄 Restoring using batch history: " + batchInfo);
                    
                    // Parse JSON manually (simple parsing for our format)
                    String[] batches = batchInfo.replace("[", "").replace("]", "").split("\\},\\{");
                    
                    for (String batchStr : batches) {
                        batchStr = batchStr.replace("{", "").replace("}", "");
                        
                        // Extract batchId and quantity
                        String[] parts = batchStr.split(",");
                        Long batchId = null;
                        int quantity = 0;
                        
                        for (String part : parts) {
                            part = part.trim();
                            if (part.contains("batchId")) {
                                String value = part.split(":")[1].trim().replace("\"", "");
                                batchId = Long.parseLong(value);
                            } else if (part.contains("quantity")) {
                                String value = part.split(":")[1].trim().replace("\"", "");
                                quantity = Integer.parseInt(value);
                            }
                        }
                        
                        if (batchId != null && quantity > 0) {
                            Optional<Batch> batchOpt = batchRepository.findById(batchId);
                            if (batchOpt.isPresent()) {
                                Batch batch = batchOpt.get();
                                batch.setStock(batch.getStock() + quantity);
                                batchRepository.save(batch);
                                
                                System.out.println("✅ Restored " + quantity + " units to batch " +
                                        batch.getBatchNumber() + " (New Stock: " + batch.getStock() + ")");
                            }
                        }
                    }
                    
                    // Update product total stock
                    int totalStock = batchRepository.findByProductId(product.getId()).stream()
                            .mapToInt(Batch::getStock)
                            .sum();
                    product.setStock(totalStock);
                    productRepository.save(product);
                    
                    System.out.println("✅ Product total stock updated to: " + totalStock);
                } else {
                    // Fallback: No batch info available (old transaction or no batches)
                    System.out.println("⚠️ No batch info available, using fallback restoration");
                    
                    List<Batch> batches = batchRepository.findByProductOrderByExpirationDate(product.getId());
                    if (!batches.isEmpty()) {
                        Batch firstBatch = batches.get(0);
                        firstBatch.setStock(firstBatch.getStock() + item.getQuantity());
                        batchRepository.save(firstBatch);
                        
                        int totalStock = batchRepository.findByProductId(product.getId()).stream()
                                .mapToInt(Batch::getStock)
                                .sum();
                        product.setStock(totalStock);
                        productRepository.save(product);
                    } else {
                        product.setStock(product.getStock() + item.getQuantity());
                        productRepository.save(product);
                    }
                }
            }
        }

        // Delete the sale
        saleRepository.deleteById(saleId);
        System.out.println("✅ Transaction deleted");
    }

    @Transactional
    public void voidTransaction(Long saleId, String reason) {
        Optional<Sale> saleOpt = saleRepository.findById(saleId);

        if (!saleOpt.isPresent()) {
            throw new RuntimeException("Transaction not found");
        }

        Sale sale = saleOpt.get();

        if (sale.isVoided()) {
            throw new RuntimeException("Transaction is already voided");
        }

        // Mark as voided
        sale.setVoided(true);
        sale.setVoidDate(LocalDateTime.now());
        sale.setVoidReason(reason != null ? reason : "No reason provided");

        // Restore inventory for all items using batch history
        for (SaleItem item : sale.getItems()) {
            List<Product> allProducts = productService.getAllProducts();
            Optional<Product> productOpt = allProducts.stream()
                    .filter(p -> p.getMedicineId().equals(item.getMedicineId()))
                    .findFirst();

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                String batchInfo = item.getBatchInfo();

                if (batchInfo != null && !batchInfo.isEmpty() && !batchInfo.equals("[]")) {
                    // ✅ PERFECT RESTORATION: Use stored batch info
                    System.out.println("🔄 Voiding: Restoring using batch history: " + batchInfo);
                    
                    try {
                        // Parse JSON manually: [{"batchId":1,"quantity":50},{"batchId":2,"quantity":30}]
                        String[] batches = batchInfo.replace("[", "").replace("]", "").split("\\},\\{");
                        
                        for (String batchStr : batches) {
                            batchStr = batchStr.replace("{", "").replace("}", "").trim();
                            
                            Long batchId = null;
                            int quantity = 0;
                            
                            // Split by comma and parse key-value pairs
                            String[] parts = batchStr.split(",");
                            for (String part : parts) {
                                part = part.trim();
                                if (part.contains("\"batchId\"")) {
                                    String value = part.split(":")[1].trim();
                                    batchId = Long.parseLong(value);
                                } else if (part.contains("\"quantity\"")) {
                                    String value = part.split(":")[1].trim();
                                    quantity = Integer.parseInt(value);
                                }
                            }
                            
                            if (batchId != null && quantity > 0) {
                                Optional<Batch> batchOpt = batchRepository.findById(batchId);
                                if (batchOpt.isPresent()) {
                                    Batch batch = batchOpt.get();
                                    batch.setStock(batch.getStock() + quantity);
                                    batchRepository.save(batch);
                                    
                                    System.out.println("✅ Restored " + quantity + " units to batch " +
                                            batch.getBatchNumber() + " (New Stock: " + batch.getStock() + ")");
                                } else {
                                    System.err.println("❌ Batch not found: " + batchId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error parsing batch info: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException("Failed to restore inventory from batch info", e);
                    }
                    
                    int totalStock = batchRepository.findByProductId(product.getId()).stream()
                            .mapToInt(Batch::getStock)
                            .sum();
                    product.setStock(totalStock);
                    productRepository.save(product);
                } else {
                    // Fallback restoration
                    List<Batch> batches = batchRepository.findByProductOrderByExpirationDate(product.getId());
                    if (!batches.isEmpty()) {
                        Batch firstBatch = batches.get(0);
                        firstBatch.setStock(firstBatch.getStock() + item.getQuantity());
                        batchRepository.save(firstBatch);
                        
                        int totalStock = batchRepository.findByProductId(product.getId()).stream()
                                .mapToInt(Batch::getStock)
                                .sum();
                        product.setStock(totalStock);
                        productRepository.save(product);
                    } else {
                        product.setStock(product.getStock() + item.getQuantity());
                        productRepository.save(product);
                    }
                }
            }
        }

        // Save the voided sale (don't delete it)
        saleRepository.save(sale);
        System.out.println("✅ Transaction voided and inventory restored");
    }

    @Transactional
    public Sale updateTransactionWithInventory(Sale sale, Map<String, Integer> originalQuantities) {
        // First, restore inventory for all original quantities
        for (Map.Entry<String, Integer> entry : originalQuantities.entrySet()) {
            String medicineId = entry.getKey();
            int originalQty = entry.getValue();

            List<Product> allProducts = productService.getAllProducts();
            Optional<Product> productOpt = allProducts.stream()
                    .filter(p -> p.getMedicineId().equals(medicineId))
                    .findFirst();

            if (productOpt.isPresent()) {
                Product product = productOpt.get();

                // Restore to batches in FEFO order (reverse the original deduction)
                List<com.inventory.Calo.s_Drugstore.entity.Batch> batches =
                        batchRepository.findByProductOrderByExpirationDate(product.getId());

                if (!batches.isEmpty()) {
                    // ✅ Add back to first batch (simulating FEFO reversal)
                    com.inventory.Calo.s_Drugstore.entity.Batch firstBatch = batches.get(0);
                    firstBatch.setStock(firstBatch.getStock() + originalQty);
                    batchRepository.save(firstBatch);

                    // Update product stock
                    product.setStock(product.getStock() + originalQty);
                    productService.updateProduct(product.getId(), product);
                } else {
                    product.setStock(product.getStock() + originalQty);
                    productService.updateProduct(product.getId(), product);
                }
            }
        }

        // Then, deduct new quantities using FEFO
        for (SaleItem item : sale.getItems()) {
            List<Product> allProducts = productService.getAllProducts();
            Optional<Product> productOpt = allProducts.stream()
                    .filter(p -> p.getMedicineId().equals(item.getMedicineId()))
                    .findFirst();

            if (productOpt.isPresent()) {
                Product product = productOpt.get();

                // Check if we have enough stock
                if (product.getStock() < item.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for " + product.getBrandName() +
                            ". Available: " + product.getStock() + ", Required: " + item.getQuantity());
                }

                // Use FEFO deduction
                productService.deductStockFromBatches(product.getId(), item.getQuantity());

                // ✅ Cleanup depleted batches
                //cleanupDepletedBatches(product.getId());
            }
        }

        // Recalculate totals
        BigDecimal newTotal = BigDecimal.ZERO;
        int newTotalItems = 0;

        for (SaleItem item : sale.getItems()) {
            newTotal = newTotal.add(item.getSubtotal());
            newTotalItems += item.getQuantity();
        }

        sale.setTotalAmount(newTotal);
        sale.setTotalItems(newTotalItems);

        // Update and return the sale
        return saleRepository.save(sale);
    }

    private void cleanupDepletedBatches(Long productId) {
        List<Batch> allBatches = batchRepository.findByProductOrderByExpirationDate(productId);

        // Filter out depleted batches (stock = 0)
        List<Batch> depletedBatches = allBatches.stream()
                .filter(batch -> batch.getStock() == 0)
                .collect(Collectors.toList());

        // Only delete if there are other batches remaining
        if (!depletedBatches.isEmpty() && depletedBatches.size() < allBatches.size()) {
            // There are still active batches, safe to delete depleted ones
            for (Batch batch : depletedBatches) {
                batchRepository.delete(batch);
                System.out.println("🗑️ Deleted depleted batch: " + batch.getBatchNumber());
            }
        } else if (!depletedBatches.isEmpty()) {
            // All batches are depleted, delete all of them
            for (Batch batch : depletedBatches) {
                batchRepository.delete(batch);
                System.out.println("🗑️ Deleted last depleted batch: " + batch.getBatchNumber());
            }
            System.out.println("⚠ All batches depleted for product ID " + productId + " - product has no stock");
        }
    }
}