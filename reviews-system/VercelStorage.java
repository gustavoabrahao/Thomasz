package com.thomaszkowalski.reviews;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Classe para gerenciar armazenamento no ambiente serverless da Vercel.
 * Esta é uma implementação temporária usando armazenamento em memória.
 * 
 * IMPORTANTE: Este armazenamento NÃO é persistente entre invocações do serverless.
 * Para uma solução real, você precisa implementar armazenamento externo
 * (MongoDB Atlas, MySQL, PostgreSQL, etc.)
 */
public class VercelStorage {
    
    // Armazenamento em memória - não persistente entre invocações
    private static final Map<Long, Review> reviews = new HashMap<>();
    private static final AtomicLong idCounter = new AtomicLong(1);
    
    /**
     * Salva uma avaliação e gera um ID se necessário
     */
    public static synchronized void saveReview(Review review) {
        if (review.getId() == 0) {
            long id = idCounter.getAndIncrement();
            review.setId(id);
        }
        reviews.put(review.getId(), review);
    }
    
    /**
     * Retorna todas as avaliações
     */
    public static synchronized List<Review> getAllReviews() {
        return new ArrayList<>(reviews.values());
    }
    
    /**
     * Retorna uma avaliação pelo ID
     */
    public static synchronized Review getReviewById(long id) {
        return reviews.get(id);
    }
    
    /**
     * Remove uma avaliação pelo ID
     */
    public static synchronized boolean removeReviewById(long id) {
        return reviews.remove(id) != null;
    }
    
    /**
     * Retorna avaliações aprovadas
     */
    public static synchronized List<Review> getApprovedReviews() {
        List<Review> result = new ArrayList<>();
        for (Review review : reviews.values()) {
            if (review.isApproved()) {
                result.add(review);
            }
        }
        return result;
    }
    
    /**
     * Retorna avaliações aprovadas positivas
     */
    public static synchronized List<Review> getPositiveApprovedReviews() {
        List<Review> result = new ArrayList<>();
        for (Review review : reviews.values()) {
            if (review.isApproved() && review.isPositive()) {
                result.add(review);
            }
        }
        return result;
    }
    
    /**
     * Retorna avaliações aprovadas negativas
     */
    public static synchronized List<Review> getNegativeApprovedReviews() {
        List<Review> result = new ArrayList<>();
        for (Review review : reviews.values()) {
            if (review.isApproved() && !review.isPositive()) {
                result.add(review);
            }
        }
        return result;
    }
    
    /**
     * Retorna avaliações pendentes
     */
    public static synchronized List<Review> getPendingReviews() {
        List<Review> result = new ArrayList<>();
        for (Review review : reviews.values()) {
            if (!review.isApproved() && !review.isRejected()) {
                result.add(review);
            }
        }
        return result;
    }
} 