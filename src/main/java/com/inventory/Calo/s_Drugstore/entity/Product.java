package com.inventory.Calo.s_Drugstore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medicine_id", unique = true, nullable = false)
    private String medicineId;

    @Column(name = "name", nullable = false)
    private String brandName;

    @Column(name = "generic_name")
    private String genericName;

    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "category")
    private String category;

//    @Column(name = "description")
//    private String description;

    @Column(name = "min_stock_level")
    private Integer minStockLevel = 10;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "prescription_required")
    private Boolean prescriptionRequired = false;

    @Column(name = "dosage_form")
    private String dosageForm;

    @Column(name = "dosage_strength")
    private String dosageStrength;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Batch> batches = new ArrayList<>();

    // Constructors
    public Product() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
        this.prescriptionRequired = false;
        this.dosageForm = "Tablet";
        this.unitOfMeasure = "Piece";
        this.minStockLevel = 10;
        this.genericName = "";
        this.brandName = "";
    }

    public Product(String medicineId, String brandName, Integer stock, BigDecimal price,
                   LocalDate expirationDate, String supplier) {
        this();
        this.medicineId = medicineId;
        this.brandName = brandName;
        this.stock = stock;
        this.price = price;
        this.expirationDate = expirationDate;
        this.supplier = supplier;
    }

    public Product(String medicineId, String brandName, Integer stock, BigDecimal price,
                   LocalDate expirationDate, String supplier, String category,
                   String batchNumber, Integer minStockLevel, Boolean prescriptionRequired,
                   String dosageForm, String dosageStrength, String manufacturer,
                   String unitOfMeasure) {
        this();
        this.medicineId = medicineId;
        this.brandName = brandName;
        this.stock = stock;
        this.price = price;
        this.expirationDate = expirationDate;
        this.supplier = supplier;
        this.category = category;
        this.batchNumber = batchNumber;
        this.minStockLevel = minStockLevel != null ? minStockLevel : 10;
        this.prescriptionRequired = prescriptionRequired != null ? prescriptionRequired : false;
        this.dosageForm = dosageForm != null ? dosageForm : "Tablet";
        this.dosageStrength = dosageStrength;
        this.manufacturer = manufacturer;
        this.unitOfMeasure = unitOfMeasure != null ? unitOfMeasure : "Piece";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(String medicineId) {
        this.medicineId = medicineId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String name) {
        this.brandName = name;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

//    public String getDescription() {
//        return description;
//    }
//
//    public void setDescription(String description) {
//        this.description = description;
//    }

    public Integer getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(Integer minStockLevel) {
        this.minStockLevel = minStockLevel;
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

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Boolean getPrescriptionRequired() {
        return prescriptionRequired;
    }

    public void setPrescriptionRequired(Boolean prescriptionRequired) {
        this.prescriptionRequired = prescriptionRequired;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getDosageStrength() {
        return dosageStrength;
    }

    public void setDosageStrength(String dosageStrength) {
        this.dosageStrength = dosageStrength;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    // Helper methods
    public boolean isLowStock() {
        return stock != null && stock <= minStockLevel;
    }

    public boolean isExpiringSoon() {
        if (expirationDate == null) return false;
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        // Only include products expiring between today and 30 days from now (not already expired)
        return !expirationDate.isBefore(today) && expirationDate.isBefore(thirtyDaysFromNow);
    }

    public boolean isExpired() {
        if (expirationDate == null) return false;
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        return expirationDate.isAfter(thirtyDaysFromNow);
    }

    public String getStockStatus() {
        if (stock == null) return "Unknown";
        return isLowStock() ? "Low" : "Good";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDate.now();
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", medicineId='" + medicineId + '\'' +
                ", name='" + brandName + '\'' +
                ", stock=" + stock +
                ", price=" + price +
                ", supplier='" + supplier + '\'' +
                '}';
    }

    //BATCH SUPPORT
    public List<Batch> getBatches() {
        return batches;
    }

    public void setBatches(List<Batch> batches) {
        this.batches = batches;
    }

    public void addBatch(Batch batch) {
        batches.add(batch);
        batch.setProduct(this);
    }

    public void removeBatch(Batch batch) {
        batches.remove(batch);
        batch.setProduct(null);
    }

    // Update the getStock() method to calculate total from all batches:
    public Integer getTotalStock() {
        if (batches == null || batches.isEmpty()) {
            return stock != null ? stock : 0;
        }
        return batches.stream()
                .mapToInt(Batch::getStock)
                .sum();
    }

    // Get the earliest expiration date from all batches
    public LocalDate getEarliestExpirationDate() {
        if (batches == null || batches.isEmpty()) {
            return expirationDate;
        }
        return batches.stream()
                .map(Batch::getExpirationDate)
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .orElse(expirationDate);
    }
    public String getName() {
        return brandName;
    }

    public void setName(String name) {
        this.brandName = name;
    }
}