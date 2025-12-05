// Added to fix the duplication issue and to have the ability to see batches

package com.inventory.Calo.s_Drugstore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "batches")
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "date_received")
    private LocalDate dateReceived;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    // Constructors
    public Batch() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
        this.dateReceived = LocalDate.now();
    }

    public Batch(String batchNumber, Product product, Integer stock,
                 LocalDate expirationDate, BigDecimal price, String supplier) {
        this();
        this.batchNumber = batchNumber;
        this.product = product;
        this.stock = stock;
        this.expirationDate = expirationDate;
        this.price = price;
        this.supplier = supplier;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public LocalDate getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(LocalDate dateReceived) {
        this.dateReceived = dateReceived;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isExpiringSoon() {
        if (expirationDate == null) return false;
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        return expirationDate.isBefore(thirtyDaysFromNow);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDate.now();
    }

    @Override
    public String toString() {
        return "Batch{" +
                "id=" + id +
                ", batchNumber='" + batchNumber + '\'' +
                ", stock=" + stock +
                ", expirationDate=" + expirationDate +
                ", price=" + price +
                ", supplier='" + supplier + '\'' +
                '}';
    }
}

