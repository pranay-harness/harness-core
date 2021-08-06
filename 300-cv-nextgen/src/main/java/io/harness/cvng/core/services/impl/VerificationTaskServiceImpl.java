
package io.harness.cvng.core.services.impl;

import static io.harness.cvng.CVConstants.DEPLOYMENT;
import static io.harness.cvng.CVConstants.LIVE_MONITORING;
import static io.harness.cvng.CVConstants.TAG_DATA_SOURCE;
import static io.harness.cvng.CVConstants.TAG_VERIFICATION_TYPE;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.CVConstants;
import io.harness.cvng.beans.DataSourceType;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.groovy.util.Maps;

public class VerificationTaskServiceImpl implements VerificationTaskService {
  @Inject private HPersistence hPersistence;
  @Inject private Clock clock;

  // TODO: optimize this and add caching support. Since this collection is immutable
  @Override
  public String create(String accountId, String cvConfigId, DataSourceType provider) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(cvConfigId);
    // TODO: Change to new generated uuid in a separate PR since it needs more validation.
    VerificationTask verificationTask =
        VerificationTask.builder()
            .uuid(cvConfigId)
            .accountId(accountId)
            .cvConfigId(cvConfigId)
            .tags(Maps.of(TAG_DATA_SOURCE, provider.name(), TAG_VERIFICATION_TYPE, LIVE_MONITORING))
            .build();
    hPersistence.save(verificationTask);
    return verificationTask.getUuid();
  }

  @Override
  public String create(String accountId, String cvConfigId, String verificationJobInstanceId, DataSourceType provider) {
    Preconditions.checkNotNull(accountId, "accountId can not be null");
    Preconditions.checkNotNull(cvConfigId, "cvConfigId can not be null");
    Preconditions.checkNotNull(verificationJobInstanceId, "verificationJobInstanceId can not be null");
    checkIfVerificationTaskAlreadyExists(accountId, cvConfigId, verificationJobInstanceId);
    VerificationTask verificationTask =
        VerificationTask.builder()
            .accountId(accountId)
            .cvConfigId(cvConfigId)
            .validUntil(Date.from(clock.instant().plus(CVConstants.VERIFICATION_JOB_INSTANCE_EXPIRY_DURATION)))
            .verificationJobInstanceId(verificationJobInstanceId)
            .tags(Maps.of(TAG_DATA_SOURCE, provider.name(), TAG_VERIFICATION_TYPE, DEPLOYMENT))
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
    Optional<String> maybeVerificationTaskId =
        maybeGetVerificationTaskId(accountId, cvConfigId, verificationJobInstanceId);
    return maybeVerificationTaskId.orElseThrow(
        () -> new IllegalStateException("VerificationTask mapping does not exist"));
  }

  private Optional<String> maybeGetVerificationTaskId(
      String accountId, String cvConfigId, String verificationJobInstanceId) {
    Preconditions.checkNotNull(verificationJobInstanceId, "verificationJobInstanceId should not be null");
    return Optional
        .ofNullable(hPersistence.createQuery(VerificationTask.class)
                        .filter(VerificationTaskKeys.accountId, accountId)
                        .filter(VerificationTaskKeys.verificationJobInstanceId, verificationJobInstanceId)
                        .filter(VerificationTaskKeys.cvConfigId, cvConfigId)
                        .get())
        .map(VerificationTask::getUuid);
  }

  @Override
  public Set<String> getVerificationTaskIds(String accountId, String verificationJobInstanceId) {
    Set<String> results = maybeGetVerificationTaskIds(accountId, verificationJobInstanceId);
    Preconditions.checkState(!results.isEmpty(), "No verification task mapping exist for verificationJobInstanceId %s",
        verificationJobInstanceId);
    return results;
  }

  @Override
  public Set<String> maybeGetVerificationTaskIds(String accountId, String verificationJobInstanceId) {
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
    hPersistence.delete(hPersistence.createQuery(VerificationTask.class)
                            .filter(VerificationTaskKeys.cvConfigId, cvConfigId)
                            .field(VerificationTaskKeys.verificationJobInstanceId)
                            .doesNotExist());
  }

  @Override
  public List<String> getVerificationTaskIds(String cvConfigId) {
    return hPersistence.createQuery(VerificationTask.class, excludeAuthority)
        .filter(VerificationTaskKeys.cvConfigId, cvConfigId)
        .project(VerificationTaskKeys.uuid, true)
        .asList()
        .stream()
        .map(verificationTask -> verificationTask.getUuid())
        .collect(Collectors.toList());
  }

  @Override
  public Optional<String> findBaselineVerificationTaskId(
      String currentVerificationTaskId, VerificationJobInstance verificationJobInstance) {
    Preconditions.checkState(verificationJobInstance.getResolvedJob() instanceof TestVerificationJob,
        "getResolvedJob has to be instance of TestVerificationJob");
    TestVerificationJob testVerificationJob = (TestVerificationJob) verificationJobInstance.getResolvedJob();
    String baselineVerificationJobInstanceId = testVerificationJob.getBaselineVerificationJobInstanceId();
    if (baselineVerificationJobInstanceId == null) {
      return Optional.empty();
    }
    String cvConfigId = getCVConfigId(currentVerificationTaskId);
    return maybeGetVerificationTaskId(
        verificationJobInstance.getAccountId(), cvConfigId, baselineVerificationJobInstanceId);
  }

  @Override
  public List<String> getAllVerificationJobInstanceIdsForCVConfig(String cvConfigId) {
    return hPersistence.createQuery(VerificationTask.class, excludeAuthority)
        .filter(VerificationTaskKeys.cvConfigId, cvConfigId)
        .field(VerificationTaskKeys.verificationJobInstanceId)
        .exists()
        .project(VerificationTaskKeys.verificationJobInstanceId, true)
        .asList()
        .stream()
        .map(verificationTask -> verificationTask.getVerificationJobInstanceId())
        .collect(Collectors.toList());
  }

  @Override
  public List<String> maybeGetVerificationTaskIds(List<String> verificationJobInstanceIds) {
    return hPersistence.createQuery(VerificationTask.class)
        .field(VerificationTaskKeys.verificationJobInstanceId)
        .in(verificationJobInstanceIds)
        .asList()
        .stream()
        .map(VerificationTask::getUuid)
        .collect(Collectors.toList());
  }
}
