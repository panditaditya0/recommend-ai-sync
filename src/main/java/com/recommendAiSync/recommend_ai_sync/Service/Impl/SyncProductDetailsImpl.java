package com.recommendAiSync.recommend_ai_sync.Service.Impl;

import com.google.common.collect.Lists;
import com.recommendAiSync.recommend_ai_sync.Model.ProductDetailsModel;
import com.recommendAiSync.recommend_ai_sync.Repo.ProductDetailsRepo;
import com.recommendAiSync.recommend_ai_sync.Service.SyncProductDetailsService;
import com.recommendAiSync.recommend_ai_sync.config.WeaviateConfig;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.api.ObjectsBatcher;
import io.weaviate.client.v1.batch.model.ObjectGetResponse;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.data.replication.model.ConsistencyLevel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.io.*;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class SyncProductDetailsImpl implements SyncProductDetailsService {
    private final String className = "TestImg19";  // Replace with your class name
    private final WeaviateConfig singleWeaviateClient;
    public Logger LOGGER = LoggerFactory.getLogger(SyncProductDetailsImpl.class);
    private final ProductDetailsRepo productDetailsRepo;

    @Override
    public void DownloadImageByCategory(String categoryName) {
        ArrayList<Long> listOfProductIds = productDetailsRepo.getListOfProductsByCategory(categoryName);
//        int size = listOfProducts.size();
//        List<List<ProductDetailsModel>> chunkOfProductDetails = Lists.partition(listOfProducts, size/5);
//        for(int i =0;i< chunkOfProductDetails.size();i++){
//            for(int j=0;j<chunkOfProductDetails.get(i).size();j++){
//                ProductDetailsModel aProduct = chunkOfProductDetails.get(i).get(j);
//                String imageLink = "https://dimension-six.perniaspopupshop.com/media/catalog/product"+ aProduct.image_link;
//                String base64Image = downloadImageAsBase64(imageLink);
//                chunkOfProductDetails.get(i).get(j).base64Image_original = base64Image;
//            }
//            productDetailsRepo.saveAll(chunkOfProductDetails.get(i));
//        }

        int size = listOfProductIds.size();
        List<List<Long>> chunkOfProductDetails = Lists.partition(listOfProductIds, size / 5);
        LOGGER.info("Number of products = " + size);
        LOGGER.info("Number of chunks = " + chunkOfProductDetails.size());


        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (List<Long> chunk : chunkOfProductDetails) {
            tasks.add(() -> {
                for (Long productId : chunk) {
                    try{
                        Thread.sleep(600);
                        ProductDetailsModel product = productDetailsRepo.getProductById(productId);
                        if(null != product.base64Image_original){
                            continue;
                        }
                        String imageLink = "https://dimension-six.perniaspopupshop.com/media/catalog/product" + product.image_link;
                        product.base64Image_original = downloadImageAsBase64(imageLink);
                        LOGGER.info("sku -> " + product.sku_id + " download image from " + imageLink);
                        productDetailsRepo.save(product);
                    } catch (Exception ex){
                        System.out.println("Error in sku "+ productId +"  "+ex.getMessage());
                        LOGGER.info("Error in sku "+ productId +"  "+ex.getMessage());
                    }
                }
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    @Override
    public void uploadImageToVectorDb(String categoryName) {
        ArrayList<Long> listOfProductIds = productDetailsRepo.getListOfProductsByCategory(categoryName);
        int size = listOfProductIds.size();
        List<List<Long>> chunkOfProductDetails = Lists.partition(listOfProductIds, size / 5);
        LOGGER.info("Number of products = " + size);
        LOGGER.info("Number of chunks = " + chunkOfProductDetails.size());
        List<List<Long>> chunks = Lists.partition(listOfProductIds, 25);
        int counter = 0;
        for (List<Long> sublist : chunks) {
            try {
                ArrayList<ProductDetailsModel> listOfKafkaProducts = productDetailsRepo.getListOfProducts(sublist);
                final List<Map<String, Object>> listOfProps = this.prepareData(listOfKafkaProducts);
                this.pushToVectorDb(listOfProps);
                counter += listOfProps.size();
                LOGGER.info("Pushed ->  " + counter);
                System.gc();
                Thread.sleep(2);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
            }
        }
    }

    public List<Map<String, Object>> prepareData(final List<ProductDetailsModel> allKafkaPayload) {
        List<Map<String, Object>> dataObjs = new ArrayList<>();
        for (ProductDetailsModel kafkaPayload : allKafkaPayload) {
            Optional productDetailsOptional = productDetailsRepo.findById(kafkaPayload.getEntity_id());
            if (productDetailsOptional.isPresent()) {
                ProductDetailsModel finalObject1 = (ProductDetailsModel) productDetailsOptional.get();
                Map<String, Object> properties = new HashMap<>();
                properties.put("image", finalObject1.base64Image_original);
                properties.put("sku_id", finalObject1.sku_id);
                properties.put("product_id", finalObject1.product_id);
                properties.put("brand", finalObject1.brand);
                properties.put("some_i", finalObject1.uuid.toString());
                properties.put("color", finalObject1.color);
                properties.put("in_stock",finalObject1.in_stock);
                properties.put("parent_categories", finalObject1.parent_categories);
                properties.put("child_categories", finalObject1.child_categories);
                properties.put("price", (float) finalObject1.price_in);
                dataObjs.add(properties);
            } else {
                LOGGER.info("No product details found for id " + kafkaPayload.entity_id);
            }
        }
        return dataObjs;
    }
    public void pushToVectorDb(List<Map<String, Object>> dataObjs) {
        if (dataObjs.size() > 0) {
            ObjectsBatcher batcher = singleWeaviateClient.weaviateClientMethod().batch().objectsBatcher();
            for (Map<String, Object> prop : dataObjs) {
                String i_d = prop.get("some_i").toString();
                prop.remove("some_i");
                batcher.withObject(WeaviateObject.builder()
                        .className(className)
                        .properties(prop)
                        .id(i_d)
                        .build());
            }
            Result<ObjectGetResponse[]> a = batcher
                    .withConsistencyLevel(ConsistencyLevel.ONE)
                    .run();

            for (ObjectGetResponse b : a.getResult()) {
                if (!(b.getResult().toString().contains
                        ("SUCCESS"))) {

                    LOGGER.error("ERROR while bulk import -> " + b.getId());
                    LOGGER.error("ERROR " + b.getResult().toString());
                } else {
                    LOGGER.info("Completed bulk import -> " + b.getId());
                }
            }
        }
    }

    public static String downloadImageAsBase64(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = url.openStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
