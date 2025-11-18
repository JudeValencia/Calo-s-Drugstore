package com.inventory.Calo.s_Drugstore.repository;

import com.inventory.Calo.s_Drugstore.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
}