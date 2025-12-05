package com.inventory.Calo.s_Drugstore.service;

import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.entity.Sale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    @Autowired
    private SalesService salesService;

    @Autowired
    private ProductService productService;

    /**
     * Get dashboard metrics (KPI cards data)
     */
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Get today's sales
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            LocalDateTime endOfToday = LocalDate.now().atTime(23, 59, 59);
            List<Sale> todaySales = salesService.getSalesBetweenDates(startOfToday, endOfToday);

            // Calculate total sales today
            BigDecimal totalSalesToday = todaySales.stream()
                    .map(Sale::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Get yesterday's sales for comparison
            LocalDateTime startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay();
            LocalDateTime endOfYesterday = LocalDate.now().minusDays(1).atTime(23, 59, 59);
            List<Sale> yesterdaySales = salesService.getSalesBetweenDates(startOfYesterday, endOfYesterday);

            BigDecimal totalSalesYesterday = yesterdaySales.stream()
                    .map(Sale::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate percentage change
            String salesChange = calculatePercentageChange(totalSalesToday, totalSalesYesterday);

            // Low stock alerts
            long lowStockCount = productService.getLowStockCount();
            String lowStockMessage = lowStockCount > 0
                    ? lowStockCount + " items need restocking"
                    : "All items well stocked";

            // Expiring medicines (within 30 days)
            List<Product> expiringProducts = productService.getExpiringProducts();
            long expiringCount = expiringProducts.size();
            String expiringMessage = expiringCount > 0
                    ? expiringCount + " products expiring soon"
                    : "No products expiring soon";

            // Total inventory count
            List<Product> allProducts = productService.getAllProducts();
            long totalInventoryCount = allProducts.size();
            long totalStock = allProducts.stream()
                    .mapToLong(Product::getStock)
                    .sum();
            String inventoryMessage = totalStock + " units in stock";

            // Put everything in the map
            metrics.put("totalSalesToday", totalSalesToday.doubleValue());
            metrics.put("salesChange", salesChange);
            metrics.put("lowStockAlerts", lowStockCount);
            metrics.put("lowStockMessage", lowStockMessage);
            metrics.put("expiringMedicines", expiringCount);
            metrics.put("expiringMessage", expiringMessage);
            metrics.put("totalInventoryCount", totalInventoryCount);
            metrics.put("inventoryMessage", inventoryMessage);

        } catch (Exception e) {
            e.printStackTrace();
            // Return default values on error
            metrics.put("totalSalesToday", 0.0);
            metrics.put("salesChange", "+0%");
            metrics.put("lowStockAlerts", 0L);
            metrics.put("lowStockMessage", "No data");
            metrics.put("expiringMedicines", 0L);
            metrics.put("expiringMessage", "No data");
            metrics.put("totalInventoryCount", 0L);
            metrics.put("inventoryMessage", "No data");
        }

        return metrics;
    }

    /**
     * Get sales trends for the last 7 days
     */
    public List<Map<String, Object>> getSalesTrends() {
        List<Map<String, Object>> trends = new ArrayList<>();

        try {
            LocalDate today = LocalDate.now();

            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);

                List<Sale> daySales = salesService.getSalesBetweenDates(startOfDay, endOfDay);
                BigDecimal dayTotal = daySales.stream()
                        .map(Sale::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("day", date.format(DateTimeFormatter.ofPattern("EEE")));
                dayData.put("sales", dayTotal.doubleValue());
                trends.add(dayData);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Return default data on error
            String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (String day : days) {
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("day", day);
                dayData.put("sales", 0.0);
                trends.add(dayData);
            }
        }

        return trends;
    }

    /**
     * Get inventory distribution by category
     */
    public Map<String, Integer> getInventoryDistribution() {
        Map<String, Integer> distribution = new HashMap<>();

        try {
            List<Product> allProducts = productService.getAllProducts();

            for (Product product : allProducts) {
                String category = product.getCategory();
                distribution.put(category, distribution.getOrDefault(category, 0) + product.getStock());
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Return default data on error
            distribution.put("Pain Relief", 0);
            distribution.put("Antibiotics", 0);
            distribution.put("Vitamins", 0);
            distribution.put("Cold & Flu", 0);
            distribution.put("Others", 0);
        }

        return distribution;
    }

    /**
     * Helper method to calculate percentage change
     */
    private String calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }

        BigDecimal difference = current.subtract(previous);
        BigDecimal percentageChange = difference
                .divide(previous, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        String sign = percentageChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + String.format("%.1f%%", percentageChange);
    }

    /**
     * Get recent activity for dashboard
     */
    /**
     * Get recent activity for dashboard
     */
    public List<Map<String, Object>> getRecentActivity() {
        List<Map<String, Object>> activities = new ArrayList<>();

        try {
            // Get today's sales (most recent first)
            List<Sale> recentSales = salesService.getTodaysTransactions();

            // Add sales to activities
            for (Sale sale : recentSales) {
                if (activities.size() >= 8) break; // Limit total activities

                Map<String, Object> activity = new HashMap<>();
                activity.put("type", "sale");
                activity.put("description", "Sale completed");

                // Get first item name from sale
                String itemName = "Multiple items";
                if (!sale.getItems().isEmpty()) {
                    itemName = sale.getItems().get(0).getMedicineName();
                    if (sale.getItems().size() > 1) {
                        itemName += " +" + (sale.getItems().size() - 1) + " more";
                    }
                }
                activity.put("details", itemName);

                activity.put("amount", "₱" + String.format("%,.2f", sale.getTotalAmount()));
                activity.put("timestamp", formatTimeAgo(sale.getCreatedAt()));
                activity.put("status", "completed");
                activities.add(activity);
            }

            // Get low stock items
            List<Product> lowStockProducts = productService.getLowStockProducts();
            for (Product product : lowStockProducts) {
                if (activities.size() >= 8) break;

                Map<String, Object> activity = new HashMap<>();
                activity.put("type", "alert");
                activity.put("description", "Low stock alert");
                activity.put("details", product.getBrandName());
                activity.put("amount", product.getStock() + " unit/s left");
                activity.put("timestamp", "Now");
                activity.put("status", "warning");
                activities.add(activity);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return activities;
    }

    /**
     * Helper method to format time ago
     */
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Just now";

        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";

        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";

        long days = hours / 24;
        return days + " day" + (days == 1 ? "" : "s") + " ago";
    }

    /**
     * Get metrics for a specific staff member
     */
    public Map<String, Object> getStaffMetrics(Long userId) {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Get today's sales
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            LocalDateTime endOfToday = LocalDate.now().atTime(23, 59, 59);
            List<Sale> todaySales = salesService.getSalesBetweenDates(startOfToday, endOfToday);

            // Filter by user ID
            List<Sale> mySales = todaySales.stream()
                    .filter(sale -> sale.getUserId().equals(userId))
                    .collect(java.util.stream.Collectors.toList());

            // Calculate staff metrics
            int myTransactions = mySales.size();
            int myItemsSold = mySales.stream()
                    .mapToInt(Sale::getTotalItems)
                    .sum();

            BigDecimal myRevenue = mySales.stream()
                    .map(Sale::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            metrics.put("myTransactions", myTransactions);
            metrics.put("myItemsSold", myItemsSold);
            metrics.put("myRevenue", myRevenue.doubleValue());

        } catch (Exception e) {
            e.printStackTrace();
            metrics.put("myTransactions", 0);
            metrics.put("myItemsSold", 0);
            metrics.put("myRevenue", 0.0);
        }

        return metrics;
    }

    /**
     * Get recent activity for a specific staff member
     */
    public List<Map<String, Object>> getStaffRecentActivity(Long userId) {
        List<Map<String, Object>> activities = new ArrayList<>();

        try {
            // Get today's sales by this staff member
            List<Sale> todaySales = salesService.getTodaysTransactions();

            // Filter by user ID
            List<Sale> mySales = todaySales.stream()
                    .filter(sale -> sale.getUserId().equals(userId))
                    .collect(java.util.stream.Collectors.toList());

            // Add sales to activities (limit to 5)
            for (Sale sale : mySales) {
                if (activities.size() >= 5) break;

                Map<String, Object> activity = new HashMap<>();
                activity.put("type", "sale");
                activity.put("description", sale.getTransactionId());

                // Get first item name
                String itemName = sale.getTotalItems() + " items";
                if (!sale.getItems().isEmpty()) {
                    itemName = sale.getItems().get(0).getMedicineName();
                    if (sale.getItems().size() > 1) {
                        itemName = sale.getTotalItems() + " items";
                    }
                }
                activity.put("details", itemName);

                activity.put("amount", "₱" + String.format("%,.2f", sale.getTotalAmount()));
                activity.put("timestamp", formatTimeAgo(sale.getCreatedAt()));
                activity.put("status", "completed");
                activities.add(activity);
            }

            // Add low stock items (limit to 3)
            List<Product> lowStockProducts = productService.getLowStockProducts();
            int count = 0;
            for (Product product : lowStockProducts) {
                if (count >= 3) break;

                Map<String, Object> activity = new HashMap<>();
                activity.put("type", "alert");
                activity.put("description", "Low stock alert");
                activity.put("details", product.getBrandName());
                activity.put("amount", product.getStock() + " units left");
                activity.put("timestamp", "Now");
                activity.put("status", "warning");
                activities.add(activity);
                count++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return activities;
    }
}