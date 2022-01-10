/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface VerificationTaskService {
  String createLiveMonitoringVerificationTask(String accountId, String cvConfigId, DataSourceType provider);
  String createSLIVerificationTask(String accountId, String sliId);
  String createDeploymentVerificationTask(
      String accountId, String cvConfigId, String verificationJobInstanceId, DataSourceType provider);
  String getCVConfigId(String verificationTaskId);
  Optional<String> maybeGetCVConfigId(String verificationTaskId);
  String getSliId(String verificationTaskId);
  String getVerificationJobInstanceId(String verificationTaskId);
  VerificationTask get(String verificationTaskId);
  Optional<VerificationTask> maybeGet(String verificationTaskId);
  String getVerificationTaskId(String accountId, String cvConfigId, String verificationJobInstanceId);
  Set<String> getVerificationTaskIds(String accountId, String verificationJobInstanceId);

  /**
   * This can return empty if mapping does not exist. Only use this if know that the mapping might not exist. Use
   * #getVerificationTasks otherwise.
   */
  Set<String> maybeGetVerificationTaskIds(String accountId, String verificationJobInstanceId);

  String getServiceGuardVerificationTaskId(String accountId, String cvConfigId);
  String getSLIVerificationTaskId(String accountId, String sliId);
  List<String> getServiceGuardVerificationTaskIds(String accountId, List<String> cvConfigIds);
  List<String> getServiceGuardVerificationTaskIds(String accountId, String cvConfigId);
  boolean isServiceGuardId(String verificationTaskId);
  void removeLiveMonitoringMappings(String accountId, String cvConfigId);
  Optional<String> findBaselineVerificationTaskId(
      String currentVerificationTaskId, VerificationJobInstance verificationJobInstance);
  List<String> getAllVerificationJobInstanceIdsForCVConfig(String cvConfigId);
  List<String> maybeGetVerificationTaskIds(List<String> verificationJobInstanceIds);
}
