package com.recommendAiSync.recommend_ai_sync.Service.Impl;

import com.google.common.collect.Lists;
import com.recommendAiSync.recommend_ai_sync.Model.ProductDetailsModel;
import com.recommendAiSync.recommend_ai_sync.Repo.ProductDetailsRepo;
import com.recommendAiSync.recommend_ai_sync.Service.SyncProductDetailsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.*;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class SyncProductDetailsImpl implements SyncProductDetailsService {

    public Logger LOGGER = LoggerFactory.getLogger(SyncProductDetailsImpl.class);
    private final ProductDetailsRepo productDetailsRepo;

    @Override
    public void DownloadImageByCategory(String categoryName) {
        ArrayList<ProductDetailsModel> listOfProducts = productDetailsRepo.getListOfProductsByCategory(categoryName);
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

        int size = listOfProducts.size();
        List<List<ProductDetailsModel>> chunkOfProductDetails = Lists.partition(listOfProducts, size / 5);
        LOGGER.info("Number of products = "+size);
        LOGGER.info("Number of chunks = "+chunkOfProductDetails.size());


        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (List<ProductDetailsModel> chunk : chunkOfProductDetails) {
            tasks.add(() -> {
                for (ProductDetailsModel product : chunk) {
                    String imageLink = "https://dimension-six.perniaspopupshop.com/media/catalog/product" + product.image_link;
                    String base64Image = downloadImageAsBase64(imageLink);
                    product.base64Image_original = base64Image;
                    LOGGER.info("sku -> "+product.sku_id+" download image from " + imageLink);
                }
                productDetailsRepo.saveAll(chunk);
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