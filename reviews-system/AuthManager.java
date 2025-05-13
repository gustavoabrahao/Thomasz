package com.thomaszkowalski.reviews;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Classe responsável pela autenticação do administrador.
 */
public class AuthManager {
    private static final String CREDENTIALS_FILE = "admin_credentials.dat";
    private String adminUsername;
    private String adminPasswordHash;
    
    public AuthManager() {
        loadCredentials();
        
        // Se não existirem credenciais, criar padrão
        if (adminUsername == null || adminPasswordHash == null) {
            setDefaultCredentials();
        }
    }
    
    /**
     * Verifica se as credenciais do usuário são válidas.
     * 
     * @param username Nome de usuário
     * @param password Senha
     * @return true se as credenciais forem válidas, false caso contrário
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        
        String passwordHash = hashPassword(password);
        return username.equals(adminUsername) && passwordHash.equals(adminPasswordHash);
    }
    
    /**
     * Altera a senha do administrador.
     * 
     * @param oldPassword Senha atual
     * @param newPassword Nova senha
     * @return true se a senha foi alterada com sucesso, false caso contrário
     */
    public boolean changePassword(String oldPassword, String newPassword) {
        if (authenticate(adminUsername, oldPassword)) {
            adminPasswordHash = hashPassword(newPassword);
            saveCredentials();
            return true;
        }
        return false;
    }
    
    /**
     * Define as credenciais padrão do administrador.
     * Usuário: admin
     * Senha: thomasz123
     */
    private void setDefaultCredentials() {
        adminUsername = "admin";
        adminPasswordHash = hashPassword("thomasz123");
        saveCredentials();
    }
    
    /**
     * Gera um hash da senha usando SHA-256.
     * 
     * @param password Senha
     * @return Hash da senha
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // Fallback simples em caso de erro
            return password;
        }
    }
    
    /**
     * Carrega as credenciais do arquivo.
     */
    private void loadCredentials() {
        File file = new File(CREDENTIALS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                adminUsername = (String) ois.readObject();
                adminPasswordHash = (String) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Salva as credenciais no arquivo.
     */
    private void saveCredentials() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CREDENTIALS_FILE))) {
            oos.writeObject(adminUsername);
            oos.writeObject(adminPasswordHash);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 