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
 * Versão adaptada para funcionar tanto localmente quanto na Vercel e MongoDB.
 */
public class ReviewManager {
    private static final String DATA_FILE = "reviews.dat";
    private List<Review> reviews;
    private AtomicLong nextId;
    private boolean isVercelEnvironment;
    private boolean useMongoDBStorage;
    
    // Construtor
    public ReviewManager() {
        this.reviews = new ArrayList<>();
        this.nextId = new AtomicLong(1);
        
        // Detectar ambiente Vercel e MongoDB
        this.isVercelEnvironment = System.getenv("VERCEL") != null;
        this.useMongoDBStorage = System.getenv("MONGODB_URI") != null;
        
        if (!isVercelEnvironment && !useMongoDBStorage) {
            loadReviews();
        }
    }
    
    // Adicionar uma nova avaliação
    public Review addReview(Review review) {
        if (useMongoDBStorage) {
            // Usar MongoDB
            MongoDBStorage.saveReview(review);
            return review;
        } else if (isVercelEnvironment) {
            // Usar armazenamento temporário na Vercel
            VercelStorage.saveReview(review);
            return review;
        } else {
            // Usar armazenamento local em arquivo
            review.setId(nextId.getAndIncrement());
            reviews.add(review);
            saveReviews();
            return review;
        }
    }
    
    // Aprovar uma avaliação
    public boolean approveReview(Long id) {
        if (useMongoDBStorage) {
            Review review = MongoDBStorage.getReviewById(id);
            if (review != null) {
                review.setApproved(true);
                MongoDBStorage.saveReview(review);
                return true;
            }
            return false;
        } else if (isVercelEnvironment) {
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
        if (useMongoDBStorage) {
            Review review = MongoDBStorage.getReviewById(id);
            if (review != null) {
                review.setRejected(true);
                MongoDBStorage.saveReview(review);
                return true;
            }
            return false;
        } else if (isVercelEnvironment) {
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
        if (useMongoDBStorage) {
            return MongoDBStorage.getAllReviews();
        } else if (isVercelEnvironment) {
            return VercelStorage.getAllReviews();
        } else {
            return new ArrayList<>(reviews);
        }
    }
    
    // Obter avaliações pendentes (não aprovadas)
    public List<Review> getPendingReviews() {
        if (useMongoDBStorage) {
            return MongoDBStorage.getPendingReviews();
        } else if (isVercelEnvironment) {
            return VercelStorage.getPendingReviews();
        } else {
            return reviews.stream()
                    .filter(review -> !review.isApproved() && !review.isRejected())
                    .collect(Collectors.toList());
        }
    }
    
    // Obter avaliações aprovadas
    public List<Review> getApprovedReviews() {
        if (useMongoDBStorage) {
            return MongoDBStorage.getApprovedReviews();
        } else if (isVercelEnvironment) {
            return VercelStorage.getApprovedReviews();
        } else {
            return reviews.stream()
                    .filter(Review::isApproved)
                    .collect(Collectors.toList());
        }
    }
    
    // Obter avaliações positivas aprovadas
    public List<Review> getPositiveApprovedReviews() {
        if (useMongoDBStorage) {
            return MongoDBStorage.getPositiveApprovedReviews();
        } else if (isVercelEnvironment) {
            return VercelStorage.getPositiveApprovedReviews();
        } else {
            return reviews.stream()
                    .filter(review -> review.isApproved() && review.isPositive())
                    .collect(Collectors.toList());
        }
    }
    
    // Obter avaliações negativas aprovadas
    public List<Review> getNegativeApprovedReviews() {
        if (useMongoDBStorage) {
            return MongoDBStorage.getNegativeApprovedReviews();
        } else if (isVercelEnvironment) {
            return VercelStorage.getNegativeApprovedReviews();
        } else {
            return reviews.stream()
                    .filter(review -> review.isApproved() && !review.isPositive())
                    .collect(Collectors.toList());
        }
    }
    
    // Obter uma avaliação pelo ID
    public Review getReviewById(Long id) {
        if (useMongoDBStorage) {
            return MongoDBStorage.getReviewById(id);
        } else if (isVercelEnvironment) {
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