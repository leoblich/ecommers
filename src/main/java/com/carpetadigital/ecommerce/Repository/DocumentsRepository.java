package com.carpetadigital.ecommerce.Repository;

import com.carpetadigital.ecommerce.entity.DocumentsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DocumentsRepository extends JpaRepository<DocumentsEntity, Long> {

    Optional<DocumentsEntity> findByTitle(String title);
    List<DocumentsEntity> findByBorradoLogicoFalse();
    List<DocumentsEntity> findTop6ByOrderByFileCreateTimeDesc();
    List<DocumentsEntity> findTop6ByOrderByCountPreViewDesc();

    @Query("SELECT d, COUNT(p) AS count FROM DocumentsEntity d " +
            "JOIN d.payments p " +
            "GROUP BY d.id " +
            "ORDER BY count DESC")
    List<Object[]> findDocumentsOrderedBySalesCount();

    @Query(value = "SELECT d AS document FROM DocumentsEntity d " +
            "JOIN d.payments p " +
            "GROUP BY d.id " +
            "ORDER BY COUNT(p) DESC",
            countQuery = "SELECT COUNT(DISTINCT d.id) FROM DocumentsEntity d " +
                    "JOIN d.payments p")
    Page<DocumentsEntity> findDocumentsOrderedBySalesCount(Pageable pageable);
    Page<DocumentsEntity> findAllByOrderByCountPreViewDesc(Pageable pageable);
    Page<DocumentsEntity> findAllByOrderByFileCreateTimeDesc(Pageable pageable);
}
