
package io.harness.cvng.core.services.impl;

import io.harness.cvng.CVConstants;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.entities.VerificationTask.VerificationTaskKeys;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VerificationTaskServiceImpl implements VerificationTaskService {
  @Inject private HPersistence hPersistence;
  @Inject private Clock clock;
  // TODO: optimize this and add caching support. Since this collection is immutable
  @Override
  public String create(String accountId, String cvConfigId) {
    // TODO: Change to new generated uuid in a separate PR since it needs more validation.
    VerificationTask verificationTask =
        VerificationTask.builder().uuid(cvConfigId).accountId(accountId).cvConfigId(cvConfigId).build();
    hPersistence.save(verificationTask);
    return verificationTask.getUuid();
  }

  @Override
  public String create(String accountId, String cvConfigId, String verificationJobInstanceId) {
    Preconditions.checkNotNull(cvConfigId, "cvConfigId can not be null");
    Preconditions.checkNotNull(verificationJobInstanceId, "verificationJobInstanceId can not be null");
    checkIfVerificationTaskAlreadyExists(accountId, cvConfigId, verificationJobInstanceId);
    VerificationTask verificationTask =
        VerificationTask.builder()
            .accountId(accountId)
            .cvConfigId(cvConfigId)
            .validUntil(Date.from(clock.instant().plus(CVConstants.VERIFICATION_JOB_INSTANCE_EXPIRY_DURATION)))
            .verificationJobInstanceId(verificationJobInstanceId)
            .build();
    hPersistence.save(verificationTask);
    return verificationTask.getUuid();
  }

  private void checkIfVerificationTaskAlreadyExists(
      String accountId, String cvConfigId, String verificationJobInstanceId) {
    Preconditions.checkState(hPersistence.createQuery(VerificationTask.class)
                                 .filter(VerificationTaskKeys.accountId, accountId)
                                 .filter(VerificationTaskKeys.cvConfigId, cvConfigId)
                                 .filter(VerificationTaskKeys.verificationJobInstanceId, verificationJobInstanceId)
                                 .get()
            == null,
        "VerificationTask already exist for accountId %s, cvConfigId %s, verificationJobInstance %s", accountId,
        cvConfigId, verificationJobInstanceId);
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
  public String getVerificationTaskId(String accountId, String cvConfigId, String verificationJobInstanceId) {
    Preconditions.checkNotNull(verificationJobInstanceId, "verificationJobInstanceId should not be null");
    return hPersistence.createQuery(VerificationTask.class)
        .filter(VerificationTaskKeys.accountId, accountId)
        .filter(VerificationTaskKeys.verificationJobInstanceId, verificationJobInstanceId)
        .filter(VerificationTaskKeys.cvConfigId, cvConfigId)
        .get()
        .getUuid();
  }

  @Override
  public Set<String> getVerificationTaskIds(String accountId, String verificationJobInstanceId) {
    Set<String> results = hPersistence.createQuery(VerificationTask.class)
                              .filter(VerificationTaskKeys.accountId, accountId)
                              .filter(VerificationTaskKeys.verificationJobInstanceId, verificationJobInstanceId)
                              .asList()
                              .stream()
                              .map(VerificationTask::getUuid)
                              .collect(Collectors.toSet());
    Preconditions.checkState(!results.isEmpty(), "No verification task mapping exist for verificationJobInstanceId %s",
        verificationJobInstanceId);
    return results;
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
  public List<String> getServiceGuardVerificationTaskIds(String accountId, List<String> cvConfigIds) {
    return hPersistence.createQuery(VerificationTask.class)
        .filter(VerificationTaskKeys.accountId, accountId)
        .field(VerificationTaskKeys.cvConfigId)
        .in(cvConfigIds)
        .field(VerificationTaskKeys.verificationJobInstanceId)
        .doesNotExist()
        .project(VerificationTaskKeys.uuid, true)
        .asList()
        .stream()
        .map(VerificationTask::getUuid)
        .collect(Collectors.toList());
  }

  @Override
  public boolean isServiceGuardId(String verificationTaskId) {
    VerificationTask verificationTask = get(verificationTaskId);
    return verificationTask.getCvConfigId() != null && verificationTask.getVerificationJobInstanceId() == null;
  }

  @Override
  public void removeCVConfigMappings(String cvConfigId) {
    hPersistence.delete(
        hPersistence.createQuery(VerificationTask.class).filter(VerificationTaskKeys.cvConfigId, cvConfigId));
  }

  @Override
  public String findBaselineVerificationTaskId(
      String currentVerificationTaskId, VerificationJobInstance verificationJobInstance) {
    Preconditions.checkState(verificationJobInstance.getResolvedJob() instanceof TestVerificationJob,
        "getResolvedJob has to be instance of TestVerificationJob");
    TestVerificationJob testVerificationJob = (TestVerificationJob) verificationJobInstance.getResolvedJob();
    String baselineVerificationJobInstanceId = testVerificationJob.getBaselineVerificationJobInstanceId();
    if (baselineVerificationJobInstanceId == null) {
      return null;
    }
    String cvConfigId = getCVConfigId(currentVerificationTaskId);
    return getVerificationTaskId(verificationJobInstance.getAccountId(), cvConfigId, baselineVerificationJobInstanceId);
  }
}
