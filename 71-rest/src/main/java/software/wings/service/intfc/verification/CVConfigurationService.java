package software.wings.service.intfc.verification;

import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 09/Oct/2018
 */
public interface CVConfigurationService {
  String saveConfiguration(String accountId, String appId, StateType stateType, Object params);
  <T extends CVConfiguration> T getConfiguration(String serviceConfigurationId);
  <T extends CVConfiguration> List<T> listConfigurations(String accountId, String appId);
  String updateConfiguration(
      String accountId, String appId, StateType stateType, Object params, String serviceConfigurationId);
  boolean deleteConfiguration(String accountId, String appId, String serviceConfigurationId);
  <T extends CVConfiguration> List<T> listConfigurations(String accountId);
}
