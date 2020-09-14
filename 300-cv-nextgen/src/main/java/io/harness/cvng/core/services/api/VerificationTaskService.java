package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.VerificationTask;

import java.util.Set;

public interface VerificationTaskService {
  String create(String accountId, String cvConfigId);
  String create(String accountId, String cvConfigId, String verificationTaskId);
  String getCVConfigId(String verificationTaskId);
  String getVerificationJobInstanceId(String verificationTaskId);
  VerificationTask get(String verificationTaskId);
  Set<String> getVerificationTaskIds(String accountId, String verificationJobInstanceIdF);
  String getServiceGuardVerificationTaskId(String accountId, String cvConfigId);
  boolean isServiceGuardId(String verificationTaskId);
}
