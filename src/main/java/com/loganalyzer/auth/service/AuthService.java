package com.loganalyzer.auth.service;

import com.loganalyzer.auth.entity.Tenant;
import com.loganalyzer.auth.entity.User;

public interface AuthService {
    Tenant registerTenant(String companyName, String adminEmail, String adminPassword);
    String login(String email, String password);
    User validateToken(String token);
}
