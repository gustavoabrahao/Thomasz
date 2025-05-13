package com.thomaszkowalski.reviews;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP simples para gerenciar as avaliações.
 */
public class ReviewServer {
    private static final int PORT = 8080;
    private HttpServer server;
    private ReviewManager reviewManager;
    private AuthManager authManager;
    
    public ReviewServer() {
        reviewManager = new ReviewManager();
        authManager = new AuthManager();
    }
    
    /**
     * Inicia o servidor.
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        
        // Endpoints
        server.createContext("/api/reviews", new ReviewHandler());
        server.createContext("/api/admin/reviews", new AdminReviewHandler());
        server.createContext("/api/admin/login", new LoginHandler());
        
        // Servir arquivos estáticos
        server.createContext("/", new StaticFileHandler());
        
        server.start();
        System.out.println("Servidor iniciado na porta " + PORT);
    }
    
    /**
     * Para o servidor.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Servidor parado.");
        }
    }
    
    /**
     * Handler para os endpoints de avaliações públicas.
     */
    private class ReviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                
                enableCORS(exchange);
                
                // Se for uma requisição OPTIONS, retornar OK
                if (method.equals("OPTIONS")) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                
                // GET /api/reviews - Retorna todas as avaliações aprovadas
                if (method.equals("GET") && path.equals("/api/reviews")) {
                    handleGetReviews(exchange);
                    return;
                }
                
                // POST /api/reviews - Adiciona uma nova avaliação
                if (method.equals("POST") && path.equals("/api/reviews")) {
                    handleAddReview(exchange);
                    return;
                }
                
                // Endpoint não encontrado
                sendResponse(exchange, 404, "Not Found");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            } finally {
                exchange.close();
            }
        }
        
        private void handleGetReviews(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            List<Review> reviews;
            
            if (query != null && query.contains("type=positive")) {
                reviews = reviewManager.getPositiveApprovedReviews();
            } else if (query != null && query.contains("type=negative")) {
                reviews = reviewManager.getNegativeApprovedReviews();
            } else {
                reviews = reviewManager.getApprovedReviews();
            }
            
            // Converter para JSON
            String json = "[" + reviews.stream()
                .map(this::reviewToJson)
                .collect(Collectors.joining(",")) + "]";
            
            sendResponse(exchange, 200, json, "application/json");
        }
        
        private void handleAddReview(HttpExchange exchange) throws IOException {
            String requestBody = new BufferedReader(new InputStreamReader(
                    exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            
            Map<String, String> params = parseJsonBody(requestBody);
            
            String establishmentName = params.get("establishmentName");
            String address = params.get("address");
            String reviewText = params.get("reviewText");
            String reviewerName = params.get("reviewerName");
            String reviewerEmail = params.get("reviewerEmail");
            String ratingStr = params.get("rating");
            String isPositiveStr = params.get("isPositive");
            
            if (establishmentName == null || address == null || reviewText == null || 
                reviewerName == null || reviewerEmail == null || ratingStr == null || isPositiveStr == null) {
                sendResponse(exchange, 400, "Missing required fields");
                return;
            }
            
            try {
                int rating = Integer.parseInt(ratingStr);
                boolean isPositive = Boolean.parseBoolean(isPositiveStr);
                
                Review review = new Review(establishmentName, address, reviewText, rating, 
                                          reviewerName, reviewerEmail, isPositive);
                
                reviewManager.addReview(review);
                
                String json = "{\"success\": true, \"message\": \"Avaliação recebida com sucesso! Aguardando aprovação.\"}";
                sendResponse(exchange, 201, json, "application/json");
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "Invalid rating format");
            }
        }
        
        private String reviewToJson(Review review) {
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
    }
    
    /**
     * Handler para os endpoints de administração.
     */
    private class AdminReviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                
                enableCORS(exchange);
                
                // Verificar autenticação
                if (!isAuthenticated(exchange)) {
                    sendResponse(exchange, 401, "Unauthorized");
                    return;
                }
                
                // Se for uma requisição OPTIONS, retornar OK
                if (method.equals("OPTIONS")) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                
                // GET /api/admin/reviews - Retorna todas as avaliações pendentes
                if (method.equals("GET") && path.equals("/api/admin/reviews")) {
                    handleGetPendingReviews(exchange);
                    return;
                }
                
                // PUT /api/admin/reviews/{id}/approve - Aprova uma avaliação
                if (method.equals("PUT") && path.matches("/api/admin/reviews/\\d+/approve")) {
                    handleApproveReview(exchange);
                    return;
                }
                
                // DELETE /api/admin/reviews/{id} - Rejeita uma avaliação
                if (method.equals("DELETE") && path.matches("/api/admin/reviews/\\d+")) {
                    handleRejectReview(exchange);
                    return;
                }
                
                // Endpoint não encontrado
                sendResponse(exchange, 404, "Not Found");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            } finally {
                exchange.close();
            }
        }
        
        private boolean isAuthenticated(HttpExchange exchange) {
            Headers headers = exchange.getRequestHeaders();
            if (headers.containsKey("Authorization")) {
                String auth = headers.getFirst("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    String token = auth.substring(7);
                    return token.equals("admin_token"); // Simplificado para este exemplo
                }
            }
            return false;
        }
        
        private void handleGetPendingReviews(HttpExchange exchange) throws IOException {
            List<Review> reviews = reviewManager.getPendingReviews();
            
            // Converter para JSON
            String json = "[" + reviews.stream()
                .map(this::reviewToJson)
                .collect(Collectors.joining(",")) + "]";
            
            sendResponse(exchange, 200, json, "application/json");
        }
        
        private void handleApproveReview(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring(path.lastIndexOf("/approve") - 1, path.lastIndexOf("/approve"));
            
            try {
                Long id = Long.parseLong(idStr);
                boolean success = reviewManager.approveReview(id);
                
                if (success) {
                    sendResponse(exchange, 200, "{\"success\": true, \"message\": \"Avaliação aprovada com sucesso.\"}", "application/json");
                } else {
                    sendResponse(exchange, 404, "{\"success\": false, \"message\": \"Avaliação não encontrada.\"}", "application/json");
                }
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "Invalid ID format");
            }
        }
        
        private void handleRejectReview(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring(path.lastIndexOf("/") + 1);
            
            try {
                Long id = Long.parseLong(idStr);
                boolean success = reviewManager.rejectReview(id);
                
                if (success) {
                    sendResponse(exchange, 200, "{\"success\": true, \"message\": \"Avaliação rejeitada com sucesso.\"}", "application/json");
                } else {
                    sendResponse(exchange, 404, "{\"success\": false, \"message\": \"Avaliação não encontrada.\"}", "application/json");
                }
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "Invalid ID format");
            }
        }
        
        private String reviewToJson(Review review) {
            return String.format(
                "{\"id\": %d, \"establishmentName\": \"%s\", \"address\": \"%s\", \"reviewText\": \"%s\", " +
                "\"rating\": %d, \"reviewerName\": \"%s\", \"reviewerEmail\": \"%s\", \"isPositive\": %b, \"approved\": %b}",
                review.getId(),
                escapeJson(review.getEstablishmentName()),
                escapeJson(review.getAddress()),
                escapeJson(review.getReviewText()),
                review.getRating(),
                escapeJson(review.getReviewerName()),
                escapeJson(review.getReviewerEmail()),
                review.isPositive(),
                review.isApproved()
            );
        }
    }
    
    /**
     * Handler para autenticação.
     */
    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                enableCORS(exchange);
                
                // Se for uma requisição OPTIONS, retornar OK
                if (method.equals("OPTIONS")) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                
                if (method.equals("POST")) {
                    String requestBody = new BufferedReader(new InputStreamReader(
                            exchange.getRequestBody(), StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));
                    
                    Map<String, String> params = parseJsonBody(requestBody);
                    
                    String username = params.get("username");
                    String password = params.get("password");
                    
                    if (username == null || password == null) {
                        sendResponse(exchange, 400, "Missing username or password");
                        return;
                    }
                    
                    if (authManager.authenticate(username, password)) {
                        // Simplificado: normalmente, você geraria um token JWT real
                        String json = "{\"success\": true, \"token\": \"admin_token\"}";
                        sendResponse(exchange, 200, json, "application/json");
                    } else {
                        sendResponse(exchange, 401, "{\"success\": false, \"message\": \"Credenciais inválidas\"}", "application/json");
                    }
                } else {
                    sendResponse(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            } finally {
                exchange.close();
            }
        }
    }
    
    /**
     * Handler para servir arquivos estáticos.
     */
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // Redirecionar a raiz para index.html
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            try {
                // Lê o arquivo do classpath
                String content = getClass().getResourceAsStream("/static" + path).toString();
                
                // Determinar o content-type
                String contentType = "text/plain";
                if (path.endsWith(".html")) {
                    contentType = "text/html";
                } else if (path.endsWith(".css")) {
                    contentType = "text/css";
                } else if (path.endsWith(".js")) {
                    contentType = "application/javascript";
                } else if (path.endsWith(".json")) {
                    contentType = "application/json";
                }
                
                sendResponse(exchange, 200, content, contentType);
            } catch (Exception e) {
                // Arquivo não encontrado
                sendResponse(exchange, 404, "File not found: " + path);
            } finally {
                exchange.close();
            }
        }
    }
    
    /**
     * Ativa o CORS para as requisições.
     */
    private void enableCORS(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    
    /**
     * Envia uma resposta para o cliente.
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        sendResponse(exchange, statusCode, response, "text/plain");
    }
    
    /**
     * Envia uma resposta para o cliente com um content-type específico.
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
    
    /**
     * Escapa caracteres especiais no JSON.
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    
    /**
     * Converte um corpo JSON em um mapa de chave-valor.
     */
    private Map<String, String> parseJsonBody(String json) {
        Map<String, String> result = new HashMap<>();
        
        // Implementação simples para análise de JSON
        // Normalmente, você usaria uma biblioteca como Jackson ou Gson
        // Este é apenas um exemplo simples
        
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1).trim();
            
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    key = key.startsWith("\"") && key.endsWith("\"") ? key.substring(1, key.length() - 1) : key;
                    
                    String value = keyValue[1].trim();
                    value = value.startsWith("\"") && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value;
                    
                    result.put(key, value);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Método main para iniciar o servidor.
     */
    public static void main(String[] args) {
        try {
            ReviewServer server = new ReviewServer();
            server.start();
            
            // Adicionar um gancho de desligamento
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Desligando o servidor...");
                server.stop();
            }));
            
            System.out.println("Servidor iniciado. Pressione Ctrl+C para parar.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 