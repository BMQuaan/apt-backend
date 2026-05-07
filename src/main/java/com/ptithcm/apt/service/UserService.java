package com.ptithcm.apt.service;

import com.ptithcm.apt.entity.User;

public interface UserService {
    User findByUsername(String username);
    User save(User user);
}
