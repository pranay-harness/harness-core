package software.wings.service.intfc.appdynamics;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsDelegateService {
  @DelegateTaskType(TaskType.APPDYNAMICS_GET_APP_TASK)
  List<NewRelicApplication> getAllApplications(
      AppDynamicsConfig appDynamicsConfig, List<EncryptedDataDetail> encryptionDetails) throws IOException;

  @DelegateTaskType(TaskType.APPDYNAMICS_GET_TIER_TASK)
  Set<AppdynamicsTier> getTiers(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      List<EncryptedDataDetail> encryptionDetails) throws IOException;

  List<AppdynamicsNode> getNodes(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId,
      List<EncryptedDataDetail> encryptionDetails) throws IOException;

  @DelegateTaskType(TaskType.APPDYNAMICS_GET_TIER_MAP)
  Set<AppdynamicsTier> getTierDependencies(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      List<EncryptedDataDetail> encryptionDetails) throws IOException;

  @DelegateTaskType(TaskType.APPDYNAMICS_CONFIGURATION_VALIDATE_TASK)
  boolean validateConfig(AppDynamicsConfig appDynamicsConfig) throws IOException;

  List<AppdynamicsMetric> getTierBTMetrics(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog)
      throws IOException, CloneNotSupportedException;

  List<AppdynamicsMetricData> getTierBTMetricData(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      String tierName, String btName, String hostName, int durantionInMinutes,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog) throws IOException;

  AppdynamicsTier getAppdynamicsTier(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId,
      List<EncryptedDataDetail> encryptionDetails) throws IOException;

  @DelegateTaskType(TaskType.APPDYNAMICS_METRIC_DATA_FOR_NODE)
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(AppDynamicsConfig appDynamicsConfig,
      List<EncryptedDataDetail> encryptionDetails, long applicationId, long tierId, String hostName, long fromTime,
      long toTime, ThirdPartyApiCallLog apiCallLog) throws IOException, CloneNotSupportedException;
}
