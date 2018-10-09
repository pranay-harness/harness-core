package software.wings.service.intfc.verification;

import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

/**
 * @author Vaibhav Tulsyan
 * 09/Oct/2018
 */
public interface CVConfigurationService {
  String saveConfiguration(StateType stateType, Object params);
  <T extends CVConfiguration> T getConfiguration(String serviceConfigurationId);
}
