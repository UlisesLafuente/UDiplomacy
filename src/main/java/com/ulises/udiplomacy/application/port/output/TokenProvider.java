package com.ulises.udiplomacy.application.port.output;

public interface TokenProvider {
    String generateToken(String userId, String role);
    boolean validateToken(String token);
    String userIdFromToken(String token);
    String roleFromToken(String token);
}
