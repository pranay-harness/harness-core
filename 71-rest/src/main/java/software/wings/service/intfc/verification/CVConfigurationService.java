package software.wings.service.intfc.verification;

import io.harness.beans.PageRequest;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 09/Oct/2018
 */
public interface CVConfigurationService extends OwnedByAccount {
  String saveConfiguration(String accountId, String appId, StateType stateType, Object params);
  <T extends CVConfiguration> T getConfiguration(String serviceConfigurationId);
  <T extends CVConfiguration> T getConfiguration(String name, String appId, String envId);
  <T extends CVConfiguration> List<T> listConfigurations(
      String accountId, String appId, String envId, StateType stateType);
  String updateConfiguration(
      String accountId, String appId, StateType stateType, Object params, String serviceConfigurationId);
  String updateConfiguration(CVConfiguration cvConfiguration, String appId);
  String saveCofiguration(CVConfiguration cvConfiguration);
  boolean deleteConfiguration(String accountId, String appId, String serviceConfigurationId);
  <T extends CVConfiguration> List<T> listConfigurations(String accountId);

  List<CVConfiguration> listConfigurations(String accountId, PageRequest<CVConfiguration> pageRequest);

  void fillInServiceAndConnectorNames(CVConfiguration cvConfiguration);
  void deleteStaleConfigs();
  boolean resetBaseline(String appId, String cvConfigId, LogsCVConfiguration logsCVConfiguration);
}
