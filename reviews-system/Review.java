package com.thomaszkowalski.reviews;

import java.time.LocalDateTime;

/**
 * Classe que representa uma avaliação enviada por um usuário.
 */
public class Review {
    private Long id;
    private String establishmentName;
    private String address;
    private String reviewText;
    private int rating; // 1-5 estrelas
    private String reviewerName;
    private String reviewerEmail;
    private boolean isPositive; // true para lista positiva, false para negativa
    private boolean approved; // se já foi aprovada pelo admin
    private boolean rejected; // se foi rejeitada pelo admin
    private LocalDateTime createdAt;
    
    // Construtor padrão
    public Review() {
        this.createdAt = LocalDateTime.now();
        this.approved = false;
    }
    
    // Construtor com parâmetros
    public Review(String establishmentName, String address, String reviewText, 
                 int rating, String reviewerName, String reviewerEmail, boolean isPositive) {
        this.establishmentName = establishmentName;
        this.address = address;
        this.reviewText = reviewText;
        this.rating = rating;
        this.reviewerName = reviewerName;
        this.reviewerEmail = reviewerEmail;
        this.isPositive = isPositive;
        this.approved = false;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEstablishmentName() {
        return establishmentName;
    }

    public void setEstablishmentName(String establishmentName) {
        this.establishmentName = establishmentName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewerEmail() {
        return reviewerEmail;
    }

    public void setReviewerEmail(String reviewerEmail) {
        this.reviewerEmail = reviewerEmail;
    }

    public boolean isPositive() {
        return isPositive;
    }

    public void setPositive(boolean positive) {
        isPositive = positive;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Review{" +
                "id=" + id +
                ", establishmentName='" + establishmentName + '\'' +
                ", address='" + address + '\'' +
                ", rating=" + rating +
                ", reviewerName='" + reviewerName + '\'' +
                ", isPositive=" + isPositive +
                ", approved=" + approved +
                '}';
    }
} 