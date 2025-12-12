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

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "physical_address")
    private String physicalAddress;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    // Constructors
    public Supplier() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }

    public Supplier(String supplierId, String companyName, String contactPerson,
                    String mobileNumber, String email, String physicalAddress) {
        this();
        this.supplierId = supplierId;
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.physicalAddress = physicalAddress;
    }

    // Getters and Setters
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

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhysicalAddress() {
        return physicalAddress;
    }

    public void setPhysicalAddress(String physicalAddress) {
        this.physicalAddress = physicalAddress;
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
                ", mobileNumber='" + mobileNumber + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
