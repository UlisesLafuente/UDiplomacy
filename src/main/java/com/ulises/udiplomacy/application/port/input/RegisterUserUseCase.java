package com.ulises.udiplomacy.application.port.input;

public interface RegisterUserUseCase {
    String execute(String username, String password, String role);
}
