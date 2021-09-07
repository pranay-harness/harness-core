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
