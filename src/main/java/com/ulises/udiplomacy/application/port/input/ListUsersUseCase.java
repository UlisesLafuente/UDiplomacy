package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.user.User;

import java.util.List;

public interface ListUsersUseCase {
    List<User> execute();
}
