package com.thomaszkowalski.reviews;

import com.sun.net.httpserver.Headers;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adaptador para o ambiente serverless da Vercel.
 * Esta classe funciona como um ponto de entrada para as solicitações HTTP na Vercel.
 */
public class VercelHandler {
    private static ReviewManager reviewManager;
    private static AuthManager authManager;
    
    // Inicialização estática para garantir que o sistema seja inicializado apenas uma vez
    static {
        reviewManager = new ReviewManager();
        authManager = new AuthManager();
        System.out.println("VercelHandler inicializado - timestamp: " + System.currentTimeMillis());
    }
    
    /**
     * Método principal que a Vercel chama para cada solicitação HTTP
     */
    public static Map<String, Object> handleRequest(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Log da requisição
            System.out.println("Nova requisição recebida: " + request);
            
            // Extrair informações da solicitação
            String method = (String) request.get("method");
            String path = (String) request.get("path");
            Map<String, Object> query = (Map<String, Object>) request.getOrDefault("query", new HashMap<>());
            Map<String, Object> body = (Map<String, Object>) request.getOrDefault("body", new HashMap<>());
            Map<String, Object> headers = (Map<String, Object>) request.getOrDefault("headers", new HashMap<>());
            
            // Configurar cabeçalhos de resposta padrão
            Map<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("Access-Control-Allow-Origin", "*");
            responseHeaders.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            responseHeaders.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            // Se for uma solicitação OPTIONS, retornar OK
            if (method.equals("OPTIONS")) {
                return createResponse(200, responseHeaders, "");
            }
            
            // Rotas para API de avaliações (públicas)
            if (path.equals("/api/reviews")) {
                if (method.equals("GET")) {
                    return handleGetReviews(query, responseHeaders);
                } else if (method.equals("POST")) {
                    return handleAddReview(body, responseHeaders);
                }
            }
            
            // Rotas para API de administração
            if (path.equals("/api/admin/login")) {
                if (method.equals("POST")) {
                    return handleLogin(body, responseHeaders);
                }
            }
            
            if (path.equals("/api/admin/reviews")) {
                // Verificar autenticação
                String token = extractToken(headers);
                if (token == null || !authManager.validateToken(token)) {
                    return createResponse(401, responseHeaders, "{\"success\": false, \"message\": \"Unauthorized\"}");
                }
                
                if (method.equals("GET")) {
                    return handleGetPendingReviews(responseHeaders);
                } else if (method.equals("PUT") && path.contains("/approve")) {
                    return handleApproveReview(body, responseHeaders);
                } else if (method.equals("PUT") && path.contains("/reject")) {
                    return handleRejectReview(body, responseHeaders);
                }
            }
            
            // Rota não encontrada
            return createResponse(404, responseHeaders, "{\"success\": false, \"message\": \"Not Found\"}");
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "Internal Server Error: " + e.getMessage() + " | " + e.getClass().getName();
            if (e.getStackTrace().length > 0) {
                errorMessage += " | at " + e.getStackTrace()[0].toString();
            }
            return createResponse(500, Map.of("Content-Type", "application/json"), 
                    "{\"success\": false, \"message\": \"" + errorMessage + "\"}");
        }
    }
    
    private static Map<String, Object> handleGetReviews(Map<String, Object> query, Map<String, String> headers) {
        List<Review> reviews;
        
        String type = (String) query.getOrDefault("type", "all");
        
        if ("positive".equals(type)) {
            reviews = reviewManager.getPositiveApprovedReviews();
        } else if ("negative".equals(type)) {
            reviews = reviewManager.getNegativeApprovedReviews();
        } else {
            reviews = reviewManager.getApprovedReviews();
        }
        
        // Converter para JSON
        String json = "[" + reviews.stream()
            .map(VercelHandler::reviewToJson)
            .collect(Collectors.joining(",")) + "]";
        
        headers.put("Content-Type", "application/json");
        return createResponse(200, headers, json);
    }
    
    private static Map<String, Object> handleAddReview(Map<String, Object> body, Map<String, String> headers) {
        try {
            System.out.println("Dados recebidos no formulário: " + body);
            
            String establishmentName = (String) body.get("establishmentName");
            String address = (String) body.get("address");
            String reviewText = (String) body.get("reviewText");
            String reviewerName = (String) body.get("reviewerName");
            String reviewerEmail = (String) body.get("reviewerEmail");
            
            // Esses podem ser enviados como números ou strings
            int rating;
            boolean isPositive;
            
            try {
                Object ratingObj = body.get("rating");
                System.out.println("Tipo do rating: " + (ratingObj != null ? ratingObj.getClass().getName() : "null") + ", valor: " + ratingObj);
                
                if (ratingObj instanceof Number) {
                    rating = ((Number) ratingObj).intValue();
                } else {
                    rating = Integer.parseInt((String) ratingObj);
                }
                
                Object isPositiveObj = body.get("isPositive");
                System.out.println("Tipo do isPositive: " + (isPositiveObj != null ? isPositiveObj.getClass().getName() : "null") + ", valor: " + isPositiveObj);
                
                if (isPositiveObj instanceof Boolean) {
                    isPositive = (Boolean) isPositiveObj;
                } else {
                    isPositive = Boolean.parseBoolean((String) isPositiveObj);
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar rating/isPositive: " + e.getMessage());
                e.printStackTrace();
                headers.put("Content-Type", "application/json");
                return createResponse(400, headers, "{\"success\": false, \"message\": \"Invalid rating format: " + e.getMessage() + "\"}");
            }
            
            if (establishmentName == null || address == null || reviewText == null || 
                reviewerName == null || reviewerEmail == null) {
                headers.put("Content-Type", "application/json");
                return createResponse(400, headers, "{\"success\": false, \"message\": \"Missing required fields\"}");
            }
            
            Review review = new Review(establishmentName, address, reviewText, rating, 
                                      reviewerName, reviewerEmail, isPositive);
            
            reviewManager.addReview(review);
            
            headers.put("Content-Type", "application/json");
            return createResponse(201, headers, 
                    "{\"success\": true, \"message\": \"Avaliação recebida com sucesso! Aguardando aprovação.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "Internal Server Error: " + e.getMessage() + " | " + e.getClass().getName();
            if (e.getStackTrace().length > 0) {
                errorMessage += " | at " + e.getStackTrace()[0].toString();
            }
            headers.put("Content-Type", "application/json");
            return createResponse(500, headers, "{\"success\": false, \"message\": \"" + errorMessage + "\"}");
        }
    }
    
    private static Map<String, Object> handleLogin(Map<String, Object> body, Map<String, String> headers) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        
        if (username == null || password == null) {
            headers.put("Content-Type", "application/json");
            return createResponse(400, headers, "{\"success\": false, \"message\": \"Missing credentials\"}");
        }
        
        String token = authManager.authenticate(username, password);
        
        if (token != null) {
            headers.put("Content-Type", "application/json");
            return createResponse(200, headers, 
                    "{\"success\": true, \"token\": \"" + token + "\"}");
        } else {
            headers.put("Content-Type", "application/json");
            return createResponse(401, headers, 
                    "{\"success\": false, \"message\": \"Invalid credentials\"}");
        }
    }
    
    private static Map<String, Object> handleGetPendingReviews(Map<String, String> headers) {
        List<Review> reviews = reviewManager.getPendingReviews();
        
        // Converter para JSON
        String json = "[" + reviews.stream()
            .map(VercelHandler::reviewToJson)
            .collect(Collectors.joining(",")) + "]";
        
        headers.put("Content-Type", "application/json");
        return createResponse(200, headers, json);
    }
    
    private static Map<String, Object> handleApproveReview(Map<String, Object> body, Map<String, String> headers) {
        Object idObj = body.get("id");
        long id;
        
        if (idObj instanceof Number) {
            id = ((Number) idObj).longValue();
        } else {
            try {
                id = Long.parseLong((String) idObj);
            } catch (Exception e) {
                headers.put("Content-Type", "application/json");
                return createResponse(400, headers, "{\"success\": false, \"message\": \"Invalid review ID\"}");
            }
        }
        
        boolean success = reviewManager.approveReview(id);
        
        headers.put("Content-Type", "application/json");
        if (success) {
            return createResponse(200, headers, 
                    "{\"success\": true, \"message\": \"Avaliação aprovada com sucesso\"}");
        } else {
            return createResponse(404, headers, 
                    "{\"success\": false, \"message\": \"Avaliação não encontrada\"}");
        }
    }
    
    private static Map<String, Object> handleRejectReview(Map<String, Object> body, Map<String, String> headers) {
        Object idObj = body.get("id");
        long id;
        
        if (idObj instanceof Number) {
            id = ((Number) idObj).longValue();
        } else {
            try {
                id = Long.parseLong((String) idObj);
            } catch (Exception e) {
                headers.put("Content-Type", "application/json");
                return createResponse(400, headers, "{\"success\": false, \"message\": \"Invalid review ID\"}");
            }
        }
        
        boolean success = reviewManager.rejectReview(id);
        
        headers.put("Content-Type", "application/json");
        if (success) {
            return createResponse(200, headers, 
                    "{\"success\": true, \"message\": \"Avaliação rejeitada com sucesso\"}");
        } else {
            return createResponse(404, headers, 
                    "{\"success\": false, \"message\": \"Avaliação não encontrada\"}");
        }
    }
    
    private static String extractToken(Map<String, Object> headers) {
        Object authHeader = headers.get("authorization");
        if (authHeader != null) {
            String authStr = (String) authHeader;
            if (authStr.startsWith("Bearer ")) {
                return authStr.substring(7);
            }
        }
        return null;
    }
    
    private static String reviewToJson(Review review) {
        return String.format(
            "{\"id\": %d, \"establishmentName\": \"%s\", \"address\": \"%s\", \"reviewText\": \"%s\", " +
            "\"rating\": %d, \"reviewerName\": \"%s\", \"isPositive\": %b}",
            review.getId(),
            escapeJson(review.getEstablishmentName()),
            escapeJson(review.getAddress()),
            escapeJson(review.getReviewText()),
            review.getRating(),
            escapeJson(review.getReviewerName()),
            review.isPositive()
        );
    }
    
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    private static Map<String, Object> createResponse(int statusCode, Map<String, String> headers, String body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", headers);
        response.put("body", body);
        return response;
    }
} 