package com.thomaszkowalski.reviews;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe para gerenciar armazenamento no MongoDB Atlas.
 * Esta implementação é persistente entre chamadas serverless.
 */
public class MongoDBStorage {
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> reviewsCollection;
    
    // Inicializar a conexão com o MongoDB
    static {
        try {
            // Obter a string de conexão do ambiente (configurada na Vercel)
            String connectionString = System.getenv("MONGODB_URI");
            
            // Se não estiver definida, usar uma string padrão para desenvolvimento local
            if (connectionString == null || connectionString.isEmpty()) {
                connectionString = "mongodb+srv://thomasz:thomaszreviewsystem@cluster0.mongodb.net/reviews_db";
                System.out.println("Usando string de conexão padrão para desenvolvimento local");
            } else {
                System.out.println("Usando string de conexão do ambiente Vercel");
            }
            
            // Mostrar o início da string de conexão para debug (ocultando parte da senha)
            String connStrForLog = connectionString;
            if (connStrForLog.contains("@")) {
                int startPos = connStrForLog.indexOf("://") + 3;
                int endPos = connStrForLog.indexOf("@");
                String credentials = connStrForLog.substring(startPos, endPos);
                String maskedCredentials = credentials.replaceAll("(:|.(?=.))", "*");
                connStrForLog = connStrForLog.replace(credentials, maskedCredentials);
            }
            System.out.println("Tentando conexão com: " + connStrForLog);
            
            // Conectar ao MongoDB com timeout explícito
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase("reviews_db");
            reviewsCollection = database.getCollection("reviews");
            
            // Testar a conexão
            Document testDoc = new Document("test", "connection");
            reviewsCollection.insertOne(testDoc);
            reviewsCollection.deleteOne(testDoc);
            
            System.out.println("Conexão com MongoDB inicializada com sucesso");
        } catch (Exception e) {
            System.err.println("ERRO CRÍTICO ao conectar ao MongoDB: " + e.getMessage());
            e.printStackTrace();
            
            if (e.getMessage().contains("connection timed out")) {
                System.err.println("Verifique as regras de firewall e se o IP da Vercel está na lista de permissões do MongoDB Atlas");
            } else if (e.getMessage().contains("authentication failed")) {
                System.err.println("Credenciais incorretas. Verifique nome de usuário e senha.");
            } else if (e.getMessage().contains("not authorized")) {
                System.err.println("Usuário não tem permissão para acessar o banco. Verifique as permissões no MongoDB Atlas.");
            }
        }
    }
    
    /**
     * Converter Review para Document
     */
    private static Document reviewToDocument(Review review) {
        Document doc = new Document();
        doc.append("id", review.getId())
           .append("establishmentName", review.getEstablishmentName())
           .append("address", review.getAddress())
           .append("reviewText", review.getReviewText())
           .append("rating", review.getRating())
           .append("reviewerName", review.getReviewerName())
           .append("reviewerEmail", review.getReviewerEmail())
           .append("isPositive", review.isPositive())
           .append("approved", review.isApproved())
           .append("rejected", review.isRejected())
           .append("createdAt", review.getCreatedAt().toString());
        
        return doc;
    }
    
    /**
     * Converter Document para Review
     */
    private static Review documentToReview(Document doc) {
        Review review = new Review();
        review.setId(doc.getLong("id"));
        review.setEstablishmentName(doc.getString("establishmentName"));
        review.setAddress(doc.getString("address"));
        review.setReviewText(doc.getString("reviewText"));
        review.setRating(doc.getInteger("rating"));
        review.setReviewerName(doc.getString("reviewerName"));
        review.setReviewerEmail(doc.getString("reviewerEmail"));
        review.setPositive(doc.getBoolean("isPositive"));
        review.setApproved(doc.getBoolean("approved"));
        
        if (doc.containsKey("rejected")) {
            review.setRejected(doc.getBoolean("rejected"));
        }
        
        // Converter a string de data de volta para LocalDateTime
        if (doc.containsKey("createdAt")) {
            review.setCreatedAt(LocalDateTime.parse(doc.getString("createdAt")));
        }
        
        return review;
    }
    
    /**
     * Gerar próximo ID
     */
    private static synchronized long getNextId() {
        // Buscar o maior ID existente
        Document maxIdDoc = reviewsCollection.find()
                .sort(new Document("id", -1))
                .limit(1)
                .first();
        
        if (maxIdDoc != null) {
            return maxIdDoc.getLong("id") + 1;
        }
        
        return 1;
    }
    
    /**
     * Salvar uma avaliação
     */
    public static synchronized void saveReview(Review review) {
        try {
            // Se não tiver ID, gerar um novo
            if (review.getId() == null || review.getId() == 0) {
                review.setId(getNextId());
            }
            
            // Converter para Document
            Document doc = reviewToDocument(review);
            
            // Verificar se já existe
            Bson filter = Filters.eq("id", review.getId());
            Document existing = reviewsCollection.find(filter).first();
            
            if (existing != null) {
                // Atualizar
                reviewsCollection.replaceOne(filter, doc);
            } else {
                // Inserir novo
                reviewsCollection.insertOne(doc);
            }
        } catch (Exception e) {
            System.err.println("Erro ao salvar avaliação: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Buscar uma avaliação pelo ID
     */
    public static Review getReviewById(long id) {
        try {
            Document doc = reviewsCollection.find(Filters.eq("id", id)).first();
            if (doc != null) {
                return documentToReview(doc);
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar avaliação: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Listar todas as avaliações
     */
    public static List<Review> getAllReviews() {
        List<Review> reviews = new ArrayList<>();
        try {
            reviewsCollection.find().forEach(doc -> reviews.add(documentToReview(doc)));
        } catch (Exception e) {
            System.err.println("Erro ao listar avaliações: " + e.getMessage());
            e.printStackTrace();
        }
        return reviews;
    }
    
    /**
     * Listar avaliações aprovadas
     */
    public static List<Review> getApprovedReviews() {
        List<Review> reviews = new ArrayList<>();
        try {
            reviewsCollection.find(Filters.eq("approved", true)).forEach(doc -> 
                reviews.add(documentToReview(doc)));
        } catch (Exception e) {
            System.err.println("Erro ao listar avaliações aprovadas: " + e.getMessage());
            e.printStackTrace();
        }
        return reviews;
    }
    
    /**
     * Listar avaliações aprovadas positivas
     */
    public static List<Review> getPositiveApprovedReviews() {
        List<Review> reviews = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                Filters.eq("approved", true),
                Filters.eq("isPositive", true)
            );
            reviewsCollection.find(filter).forEach(doc -> 
                reviews.add(documentToReview(doc)));
        } catch (Exception e) {
            System.err.println("Erro ao listar avaliações positivas: " + e.getMessage());
            e.printStackTrace();
        }
        return reviews;
    }
    
    /**
     * Listar avaliações aprovadas negativas
     */
    public static List<Review> getNegativeApprovedReviews() {
        List<Review> reviews = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                Filters.eq("approved", true),
                Filters.eq("isPositive", false)
            );
            reviewsCollection.find(filter).forEach(doc -> 
                reviews.add(documentToReview(doc)));
        } catch (Exception e) {
            System.err.println("Erro ao listar avaliações negativas: " + e.getMessage());
            e.printStackTrace();
        }
        return reviews;
    }
    
    /**
     * Listar avaliações pendentes
     */
    public static List<Review> getPendingReviews() {
        List<Review> reviews = new ArrayList<>();
        try {
            Bson filter = Filters.and(
                Filters.eq("approved", false),
                Filters.ne("rejected", true)
            );
            reviewsCollection.find(filter).forEach(doc -> 
                reviews.add(documentToReview(doc)));
        } catch (Exception e) {
            System.err.println("Erro ao listar avaliações pendentes: " + e.getMessage());
            e.printStackTrace();
        }
        return reviews;
    }
    
    /**
     * Remover uma avaliação
     */
    public static boolean removeReviewById(long id) {
        try {
            return reviewsCollection.deleteOne(Filters.eq("id", id)).getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("Erro ao remover avaliação: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 