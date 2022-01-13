/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.verification;

import io.harness.beans.PageRequest;

import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TimeSeriesKeyTransactions;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import java.util.List;
import java.util.Map;

/**
 * @author Vaibhav Tulsyan
 * 09/Oct/2018
 */
public interface CVConfigurationService extends OwnedByAccount, OwnedByEnvironment {
  String saveConfiguration(String accountId, String appId, StateType stateType, Object params);

  String saveConfiguration(String accountId, String appId, StateType stateType, Object params, boolean createdFromYaml);

  <T extends CVConfiguration> T getConfiguration(String serviceConfigurationId);

  <T extends CVConfiguration> T getConfiguration(String name, String appId, String envId);
  <T extends CVConfiguration> List<T> listConfigurations(
      String accountId, String appId, String envId, StateType stateType);
  <T extends CVConfiguration> List<T> listConfigurations(String accountId, List<String> appIds, List<String> envIds);
  String updateConfiguration(
      String accountId, String appId, StateType stateType, Object params, String serviceConfigurationId);
  String updateConfiguration(CVConfiguration cvConfiguration, String appId);
  CVConfiguration saveToDatabase(CVConfiguration cvConfiguration, boolean createdFromYaml);
  boolean deleteConfiguration(String accountId, String appId, String serviceConfigurationId);
  boolean deleteConfiguration(String accountId, String appId, String serviceConfigurationId, boolean isSyncFromGit);
  <T extends CVConfiguration> List<T> listConfigurations(String accountId);

  List<CVConfiguration> listConfigurations(String accountId, PageRequest<CVConfiguration> pageRequest);

  void fillInServiceAndConnectorNames(CVConfiguration cvConfiguration);

  Map<String, TimeSeriesMetricDefinition> getMetricDefinitionMap(StateType stateType, CVConfiguration cvConfiguration);

  void deleteStaleConfigs();
  String resetBaseline(String appId, String cvConfigId, LogsCVConfiguration logsCVConfiguration);
  boolean updateAlertSettings(String cvConfigId, CVConfiguration cvConfiguration);
  boolean updateSnooze(String cvConfigId, CVConfiguration cvConfiguration);

  void cloneServiceGuardConfigs(String sourceEnvID, String targetEnvID);

  Map<String, String> getTxnMetricPairsForAPMCVConfig(String cvConfigId);
  boolean saveKeyTransactionsForCVConfiguration(String accountId, String cvConfigId, List<String> keyTransactions);
  boolean addToKeyTransactionsForCVConfiguration(String accountId, String cvConfigId, List<String> keyTransaction);
  boolean removeFromKeyTransactionsForCVConfiguration(String cvConfigId, List<String> keyTransaction);
  TimeSeriesKeyTransactions getKeyTransactionsForCVConfiguration(String cvConfigId);
  List<Boolean> is24x7GuardEnabledForAccounts(List<String> accountIdList);
  void disableConfig(String cvConfigId);
  List<CVConfiguration> obtainCVConfigurationsReferencedByService(String appId, String serviceId);

  void deleteConfigurationsForEnvironment(String appId, String envId);
}
