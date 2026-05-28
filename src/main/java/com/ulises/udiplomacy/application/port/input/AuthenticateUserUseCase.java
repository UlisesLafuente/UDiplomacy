package com.ulises.udiplomacy.application.port.input;

public interface AuthenticateUserUseCase {
    String execute(String username, String password);
}
