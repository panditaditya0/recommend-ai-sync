package com.recommendAiSync.recommend_ai_sync.Repo;

import com.recommendAiSync.recommend_ai_sync.Model.ProductDetailsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface ProductDetailsRepo extends JpaRepository<ProductDetailsModel, Long> {
    @Query(value = "select * from product_details_3 where ?1 =ANY(parent_categories)",  nativeQuery = true)
    ArrayList<ProductDetailsModel> getListOfProductsByCategory(String categoryName);
}
