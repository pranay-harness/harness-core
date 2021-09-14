/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.signup.entities.SignupVerificationToken;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface SignupVerificationTokenRepository extends CrudRepository<SignupVerificationToken, String> {
  Optional<SignupVerificationToken> findByToken(String token);
  Optional<SignupVerificationToken> findByUserId(String userId);
  Optional<SignupVerificationToken> findByEmail(String email);
}
