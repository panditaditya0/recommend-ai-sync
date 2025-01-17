package com.recommendAiSync.recommend_ai_sync.Repo;

import com.recommendAiSync.recommend_ai_sync.Model.ProductDetailsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface ProductDetailsRepo extends JpaRepository<ProductDetailsModel, Long> {
    @Query(value = "select entity_id from product_details_3 where ?1 =ANY(parent_categories)",  nativeQuery = true)
    ArrayList<Long> getListOfProductsByCategory(String categoryName);

    @Query(value = "select * from product_details_3 where entity_id = ?1",  nativeQuery = true)
    ProductDetailsModel getProductById(Long categoryName);

    @Query(value = "select * from product_details_3 where entity_id in  (:sublist)",  nativeQuery = true)
    ArrayList<ProductDetailsModel> getListOfProducts(@Param("sublist") List<Long> sublist, Pageable pageable);
}
