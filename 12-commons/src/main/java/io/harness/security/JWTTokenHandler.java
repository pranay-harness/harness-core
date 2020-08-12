package io.harness.security;

public interface JWTTokenHandler { boolean validate(String token, String secret); }
