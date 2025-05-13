package com.thomaszkowalski.reviews;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Classe responsável por gerenciar as avaliações.
 * Esta classe serve como um DAO simples para persistir e recuperar avaliações.
 * 
 * Versão adaptada para funcionar tanto localmente quanto na Vercel.
 */
public class ReviewManager {
    private static final String DATA_FILE = "reviews.dat";
    private List<Review> reviews;
    private AtomicLong nextId;
    private boolean isVercelEnvironment;
    
    // Construtor
    public ReviewManager() {
        this.reviews = new ArrayList<>();
        this.nextId = new AtomicLong(1);
        
        // Detectar ambiente Vercel
        this.isVercelEnvironment = System.getenv("VERCEL") != null;
        
        if (!isVercelEnvironment) {
            loadReviews();
        }
    }
    
    // Adicionar uma nova avaliação
    public Review addReview(Review review) {
        if (isVercelEnvironment) {
            // No ambiente Vercel, usar o armazenamento estático
            VercelStorage.saveReview(review);
            return review;
        } else {
            // Localmente, continuar usando o arquivo
            review.setId(nextId.getAndIncrement());
            reviews.add(review);
            saveReviews();
            return review;
        }
    }
    
    // Aprovar uma avaliação
    public boolean approveReview(Long id) {
        if (isVercelEnvironment) {
            Review review = VercelStorage.getReviewById(id);
            if (review != null) {
                review.setApproved(true);
                VercelStorage.saveReview(review);
                return true;
            }
            return false;
        } else {
            for (Review review : reviews) {
                if (review.getId().equals(id)) {
                    review.setApproved(true);
                    saveReviews();
                    return true;
                }
            }
            return false;
        }
    }
    
    // Rejeitar uma avaliação
    public boolean rejectReview(Long id) {
        if (isVercelEnvironment) {
            Review review = VercelStorage.getReviewById(id);
            if (review != null) {
                review.setRejected(true);
                VercelStorage.saveReview(review);
                return true;
            }
            return false;
        } else {
            boolean removed = reviews.removeIf(review -> review.getId().equals(id));
            if (removed) {
                saveReviews();
            }
            return removed;
        }
    }
    
    // Obter todas as avaliações
    public List<Review> getAllReviews() {
        if (isVercelEnvironment) {
            return VercelStorage.getAllReviews();
        } else {
            return new ArrayList<>(reviews);
        }
    }
    
    // Obter avaliações pendentes (não aprovadas)
    public List<Review> getPendingReviews() {
        if (isVercelEnvironment) {
            return VercelStorage.getPendingReviews();
        } else {
            return reviews.stream()
                    .filter(review -> !review.isApproved() && !review.isRejected())
                    .collect(Collectors.toList());
        }
    }
    
    // Obter avaliações aprovadas
    public List<Review> getApprovedReviews() {
        if (isVercelEnvironment) {
            return VercelStorage.getApprovedReviews();
        } else {
            return reviews.stream()
                    .filter(Review::isApproved)
                    .collect(Collectors.toList());
        }
    }
    
    // Obter avaliações positivas aprovadas
    public List<Review> getPositiveApprovedReviews() {
        if (isVercelEnvironment) {
            return VercelStorage.getPositiveApprovedReviews();
        } else {
            return reviews.stream()
                    .filter(review -> review.isApproved() && review.isPositive())
                    .collect(Collectors.toList());
        }
    }
    
    // Obter avaliações negativas aprovadas
    public List<Review> getNegativeApprovedReviews() {
        if (isVercelEnvironment) {
            return VercelStorage.getNegativeApprovedReviews();
        } else {
            return reviews.stream()
                    .filter(review -> review.isApproved() && !review.isPositive())
                    .collect(Collectors.toList());
        }
    }
    
    // Obter uma avaliação pelo ID
    public Review getReviewById(Long id) {
        if (isVercelEnvironment) {
            return VercelStorage.getReviewById(id);
        } else {
            return reviews.stream()
                    .filter(review -> review.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }
    
    // Carregar avaliações do arquivo (apenas ambiente local)
    @SuppressWarnings("unchecked")
    private void loadReviews() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                reviews = (List<Review>) ois.readObject();
                
                // Encontrar o maior ID para definir o próximo
                long maxId = 0;
                for (Review review : reviews) {
                    if (review.getId() > maxId) {
                        maxId = review.getId();
                    }
                }
                nextId.set(maxId + 1);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                // Em caso de erro, começar com uma lista vazia
                reviews = new ArrayList<>();
            }
        }
    }
    
    // Salvar avaliações no arquivo (apenas ambiente local)
    private void saveReviews() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(reviews);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 