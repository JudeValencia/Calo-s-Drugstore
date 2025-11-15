package com.inventory.Calo.s_Drugstore.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardService - Provides all the data needed for the admin dashboard
 *
 * WHAT THIS FILE DOES:
 * - Supplies metrics (numbers) for the 4 KPI cards
 * - Provides sales trend data for the line chart
 * - Provides inventory distribution for the bar chart
 * - Provides recent activity/transaction data
 *
 * NOTE: Right now it uses MOCK DATA (fake data for testing)
 * Later, you'll replace this with real database queries
 */

@Service  // This tells Spring Boot: "This is a service class, manage it for me"
public class DashboardService {

    /**
     * GET DASHBOARD METRICS (For the 4 KPI Cards)
     *
     * Returns the main numbers shown on dashboard:
     * - Total Sales Today: $2,847
     * - Low Stock Alerts: 8 items
     * - Expiring Medicines: 12 items
     * - Total Inventory Count: 2,147 items
     */
    public Map<String, Object> getDashboardMetrics() {
        // Create a Map (like a dictionary) to store all metrics
        Map<String, Object> metrics = new HashMap<>();

        // KPI Card 1: Total Sales Today
        metrics.put("totalSalesToday", 2847.0);
        metrics.put("salesChange", "+12% from yesterday");

        // KPI Card 2: Low Stock Alerts
        metrics.put("lowStockAlerts", 8);
        metrics.put("lowStockMessage", "Items need restocking");

        // KPI Card 3: Expiring Medicines
        metrics.put("expiringMedicines", 12);
        metrics.put("expiringMessage", "Expiring within 30 days");

        // KPI Card 4: Total Inventory Count
        metrics.put("totalInventoryCount", 2147);
        metrics.put("inventoryMessage", "Items in stock");

        // Return all the metrics in one package
        return metrics;
    }

    /**
     * GET SALES TRENDS (For the Line Chart)
     *
     * Returns sales data for the last 7 days
     * Used to draw the green line chart showing daily sales
     */
    public List<Map<String, Object>> getSalesTrends() {
        // Create a list to hold 7 days of sales data
        List<Map<String, Object>> trends = new ArrayList<>();

        // Days of the week
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        // Mock sales amounts for each day (in dollars)
        double[] sales = {1200, 1900, 1500, 2200, 2450, 2150, 1800};

        // Create a data point for each day
        for (int i = 0; i < days.length; i++) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("day", days[i]);        // Day name (Mon, Tue, etc.)
            dayData.put("sales", sales[i]);     // Sales amount for that day
            trends.add(dayData);
        }

        return trends;
    }

    /**
     * GET INVENTORY DISTRIBUTION (For the Bar Chart)
     *
     * Returns inventory count by category
     * Used to draw the blue bar chart showing stock levels per category
     */
    public Map<String, Integer> getInventoryDistribution() {
        // Create a Map to store category names and their stock counts
        Map<String, Integer> distribution = new HashMap<>();

        // Category: Stock Count
        distribution.put("Antibiotics", 120);
        distribution.put("Pain Relief", 85);
        distribution.put("Vitamins", 205);
        distribution.put("First Aid", 45);
        distribution.put("Cold & Flu", 90);

        return distribution;
    }

    /**
     * GET RECENT ACTIVITY (For the Recent Activity Section)
     *
     * Returns the latest transactions and inventory updates
     * Shows things like "Sale completed - Aspirin 500mg - $12.50 - 2 minutes ago"
     */
    public List<Map<String, Object>> getRecentActivity() {
        // Create a list to hold activity items
        List<Map<String, Object>> activities = new ArrayList<>();

        // Activity 1: Recent sale
        Map<String, Object> activity1 = new HashMap<>();
        activity1.put("type", "SALE");                      // Type: SALE, STOCK_UPDATE, LOW_STOCK
        activity1.put("title", "Sale completed");
        activity1.put("description", "Aspirin 500mg");
        activity1.put("amount", "$12.50");
        activity1.put("time", "2 minutes ago");
        activities.add(activity1);

        // Activity 2: Stock update
        Map<String, Object> activity2 = new HashMap<>();
        activity2.put("type", "STOCK_UPDATE");
        activity2.put("title", "Stock updated");
        activity2.put("description", "Vitamin D3");
        activity2.put("amount", "+50 units");
        activity2.put("time", "15 minutes ago");
        activities.add(activity2);

        // Activity 3: Low stock alert
        Map<String, Object> activity3 = new HashMap<>();
        activity3.put("type", "LOW_STOCK");
        activity3.put("title", "Low stock alert");
        activity3.put("description", "Cough Syrup");
        activity3.put("amount", "5 units left");
        activity3.put("time", "1 hour ago");
        activities.add(activity3);

        // Activity 4: Another sale
        Map<String, Object> activity4 = new HashMap<>();
        activity4.put("type", "SALE");
        activity4.put("title", "Sale completed");
        activity4.put("description", "Paracetamol 250mg");
        activity4.put("amount", "$8.75");
        activity4.put("time", "2 hours ago");
        activities.add(activity4);

        return activities;
    }

    /**
     * HELPER METHOD: Get detailed sales by category (for the pie chart from original design)
     *
     * This returns what percentage of sales came from each medicine category
     * Example: Pain Relief = 35%, Antibiotics = 25%, etc.
     */
    public Map<String, Double> getSalesByCategory() {
        Map<String, Double> salesByCategory = new HashMap<>();

        // Category: Percentage of total sales
        salesByCategory.put("Pain Relief", 35.0);
        salesByCategory.put("Antibiotics", 25.0);
        salesByCategory.put("Vitamins", 20.0);
        salesByCategory.put("Cold & Flu", 15.0);
        salesByCategory.put("Others", 5.0);

        return salesByCategory;
    }
}