package software.wings.service.intfc.appdynamics;

import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsService {
  List<NewRelicApplication> getApplications(@NotNull String settingId) throws IOException;

  Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId) throws IOException;

  Set<AppdynamicsTier> getDependentTiers(String settingId, long appdynamicsAppId, AppdynamicsTier tier)
      throws IOException;

  /**
   * Method to validate the appdynamics config.
   * @param settingAttribute
   * @return
   */
  boolean validateConfig(@NotNull SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Api to fetch metric data for given node.
   * @param appdynamicsSetupTestNodeData
   * @return
   */
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      AppdynamicsSetupTestNodeData appdynamicsSetupTestNodeData);
}
