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

            // ✅ NEW: Deduct from batches using FEFO instead of just updating product stock
            productService.deductStockFromBatches(product.getId(), item.getQuantity());
            //cleanupDepletedBatches(product.getId());

            // Create new sale item (don't reuse cart item)
            SaleItem saleItem = new SaleItem();
            saleItem.setMedicineId(product.getMedicineId());
            saleItem.setMedicineName(product.getBrandName());
            saleItem.setQuantity(item.getQuantity());
            saleItem.setUnitPrice(item.getUnitPrice());
            saleItem.setSubtotal(item.getSubtotal());

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

                // Get all batches (including depleted ones)
                List<Batch> batches = batchRepository.findByProductOrderByExpirationDate(product.getId());

                if (!batches.isEmpty()) {
                    // ✅ SMART RESTORE: Fill depleted batches first, then active ones
                    int remainingToRestore = item.getQuantity();

                    // Separate depleted and active batches
                    List<Batch> depletedBatches = new java.util.ArrayList<>();
                    List<Batch> activeBatches = new java.util.ArrayList<>();

                    for (Batch batch : batches) {
                        if (batch.getStock() == 0) {
                            depletedBatches.add(batch);
                        } else {
                            activeBatches.add(batch);
                        }
                    }

                    System.out.println("Found " + depletedBatches.size() + " depleted batches and " +
                            activeBatches.size() + " active batches");

                    // ✅ Strategy 1: Fill depleted batches first (in FEFO order)
                    for (Batch batch : depletedBatches) {
                        if (remainingToRestore <= 0) break;

                        // Add to this depleted batch
                        batch.setStock(batch.getStock() + remainingToRestore);
                        batchRepository.save(batch);

                        System.out.println("✅ Restored " + remainingToRestore + " units to depleted batch " +
                                batch.getBatchNumber() + " (Expiry: " + batch.getExpirationDate() +
                                ", Supplier: " + batch.getSupplier() + ")");

                        remainingToRestore = 0; // All restored to first depleted batch
                        break; // Only fill the first depleted batch
                    }

                    // ✅ Strategy 2: If no depleted batches, add to first active batch
                    if (remainingToRestore > 0 && !activeBatches.isEmpty()) {
                        Batch firstActive = activeBatches.get(0);
                        firstActive.setStock(firstActive.getStock() + remainingToRestore);
                        batchRepository.save(firstActive);

                        System.out.println("✅ Restored " + remainingToRestore + " units to active batch " +
                                firstActive.getBatchNumber());

                        remainingToRestore = 0;
                    }

                    // Update product total stock
                    int totalStock = batchRepository.findByProductId(product.getId()).stream()
                            .mapToInt(Batch::getStock)
                            .sum();
                    product.setStock(totalStock);
                    productRepository.save(product);

                    System.out.println("✅ Product total stock updated to: " + totalStock);

                } else {
                    // No batches - just update product stock
                    product.setStock(product.getStock() + item.getQuantity());
                    productRepository.save(product);
                }
            }
        }

        // Delete the sale
        saleRepository.deleteById(saleId);
        System.out.println("✅ Transaction deleted");
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