package com.inventory.Calo.s_Drugstore.service;

import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.entity.Sale;
import com.inventory.Calo.s_Drugstore.entity.SaleItem;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SalesService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductService productService;

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

            // Update product stock
            product.setStock(product.getStock() - item.getQuantity());
            productService.updateProduct(product.getId(), product);

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

    // THIS METHOD EXISTS - MAKE SURE IT'S IN YOUR FILE
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
                // Restore stock
                product.setStock(product.getStock() + item.getQuantity());
                productService.updateProduct(product.getId(), product);

                System.out.println("✅ Restored " + item.getQuantity() + " units of " +
                        product.getBrandName() + " (New stock: " + product.getStock() + ")");
            }
        }

        // Delete the sale
        saleRepository.deleteById(saleId);
        System.out.println("✅ Transaction " + sale.getTransactionId() + " deleted");

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
                // Restore the original quantity
                product.setStock(product.getStock() + originalQty);
                productService.updateProduct(product.getId(), product);
            }
        }

        // Then, deduct new quantities
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

                // Deduct the new quantity
                product.setStock(product.getStock() - item.getQuantity());
                productService.updateProduct(product.getId(), product);
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
}