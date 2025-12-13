package com.inventory.Calo.s_Drugstore.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", unique = true, nullable = false)
    private String supplierId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "email")
    private String email;

    @Column(name = "products_supplied")
    private String productsSupplied;

    @Column(name = "status", nullable = false)
    private String status = "Active";

    @Column(name = "date_added")
    private LocalDate dateAdded;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    public Supplier() {
        this.dateAdded = LocalDate.now();
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }

    public Supplier(String supplierId, String companyName, String contactNumber,
                    String address, String contactPerson, String email,
                    String productsSupplied, String status) {
        this();
        this.supplierId = supplierId;
        this.companyName = companyName;
        this.contactNumber = contactNumber;
        this.address = address;
        this.contactPerson = contactPerson;
        this.email = email;
        this.productsSupplied = productsSupplied;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProductsSupplied() {
        return productsSupplied;
    }

    public void setProductsSupplied(String productsSupplied) {
        this.productsSupplied = productsSupplied;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
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

    public String getSupplierName() {
        return companyName;
    }

    public void setSupplierName(String supplierName) {
        this.companyName = supplierName;
    }

    public String getMobileNumber() {
        return contactNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.contactNumber = mobileNumber;
    }

    public String getPhysicalAddress() {
        return address;
    }

    public void setPhysicalAddress(String physicalAddress) {
        this.address = physicalAddress;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDate.now();
    }

    @Override
    public String toString() {
        return "Supplier{" +
                "id=" + id +
                ", supplierId='" + supplierId + '\'' +
                ", companyName='" + companyName + '\'' +
                ", contactPerson='" + contactPerson + '\'' +
                ", contactNumber='" + contactNumber + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}