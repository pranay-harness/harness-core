
package io.harness.cvng.core.services.impl;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.entities.VerificationTask.VerificationTaskKeys;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.persistence.HPersistence;

import java.util.Set;
import java.util.stream.Collectors;

public class VerificationTaskServiceImpl implements VerificationTaskService {
  @Inject private HPersistence hPersistence;
  // TODO: optimize this and add caching support. Since this collection is immutable
  @Override
  public String create(String accountId, String cvConfigId) {
    VerificationTask verificationTask =
        VerificationTask.builder().uuid(cvConfigId).accountId(accountId).cvConfigId(cvConfigId).build();
    hPersistence.save(verificationTask);
    return verificationTask.getUuid();
  }

  @Override
  public String create(String accountId, String cvConfigId, String verificationJobInstanceId) {
    Preconditions.checkNotNull(cvConfigId, "cvConfigId can not be null");
    Preconditions.checkNotNull(verificationJobInstanceId, "verificationJobInstanceId can not be null");
    VerificationTask verificationTask = VerificationTask.builder()
                                            .accountId(accountId)
                                            .cvConfigId(cvConfigId)
                                            .verificationJobInstanceId(verificationJobInstanceId)
                                            .build();
    hPersistence.save(verificationTask);
    return verificationTask.getUuid();
  }

  @Override
  public String getCVConfigId(String verificationTaskId) {
    return get(verificationTaskId).getCvConfigId();
  }

  @Override
  public String getVerificationJobInstanceId(String verificationTaskId) {
    return get(verificationTaskId).getVerificationJobInstanceId();
  }

  @Override
  public VerificationTask get(String verificationTaskId) {
    VerificationTask verificationTask = hPersistence.get(VerificationTask.class, verificationTaskId);
    Preconditions.checkNotNull(verificationTask, "Invalid verificationTaskId. Verification mapping does not exist.");
    return verificationTask;
  }

  @Override
  public Set<String> getVerificationTaskIds(String accountId, String verificationJobInstanceId) {
    return hPersistence.createQuery(VerificationTask.class)
        .filter(VerificationTaskKeys.accountId, accountId)
        .filter(VerificationTaskKeys.verificationJobInstanceId, verificationJobInstanceId)
        .asList()
        .stream()
        .map(VerificationTask::getUuid)
        .collect(Collectors.toSet());
  }

  @Override
  public String getServiceGuardVerificationTaskId(String accountId, String cvConfigId) {
    VerificationTask result = hPersistence.createQuery(VerificationTask.class)
                                  .filter(VerificationTaskKeys.accountId, accountId)
                                  .filter(VerificationTaskKeys.cvConfigId, cvConfigId)
                                  .field(VerificationTaskKeys.verificationJobInstanceId)
                                  .doesNotExist()
                                  .get();
    Preconditions.checkNotNull(
        result, "VerificationTask mapping does not exist for cvConfigId %s. Please check cvConfigId", cvConfigId);
    return result.getUuid();
  }

  @Override
  public boolean isServiceGuardId(String verificationTaskId) {
    VerificationTask verificationTask = get(verificationTaskId);
    return verificationTask.getCvConfigId() != null && verificationTask.getVerificationJobInstanceId() == null;
  }
}
