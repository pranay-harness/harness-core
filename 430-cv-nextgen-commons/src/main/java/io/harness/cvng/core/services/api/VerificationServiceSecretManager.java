package io.harness.cvng.core.services.api;

public interface VerificationServiceSecretManager {
  void initializeServiceSecretKeys();
  String getVerificationServiceSecretKey();
  String getLearningEngineServiceSecret();
}
