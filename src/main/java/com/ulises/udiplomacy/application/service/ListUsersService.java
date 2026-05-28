package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.ListUsersUseCase;
import com.ulises.udiplomacy.application.port.output.UserRepository;
import com.ulises.udiplomacy.domain.user.User;

import java.util.List;

public class ListUsersService implements ListUsersUseCase {
    private final UserRepository repository;

    public ListUsersService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<User> execute() {
        return repository.findAll();
    }
}
