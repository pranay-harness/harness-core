package software.wings.service.intfc.appdynamics;

import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsDelegateService {
  @DelegateTaskType(TaskType.APPDYNAMICS_GET_APP_TASK)
  List<NewRelicApplication> getAllApplications(
      AppDynamicsConfig appDynamicsConfig, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.APPDYNAMICS_GET_TIER_TASK)
  Set<AppdynamicsTier> getTiers(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog);

  Set<AppdynamicsNode> getNodes(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog);

  @DelegateTaskType(TaskType.APPDYNAMICS_GET_TIER_MAP)
  Set<AppdynamicsTier> getTierDependencies(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog);

  @DelegateTaskType(TaskType.APPDYNAMICS_CONFIGURATION_VALIDATE_TASK)
  boolean validateConfig(AppDynamicsConfig appDynamicsConfig, List<EncryptedDataDetail> encryptedDataDetails);

  List<AppdynamicsMetric> getTierBTMetrics(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog);

  List<AppdynamicsMetricData> getTierBTMetricData(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId,
      String tierName, String btName, String hostName, Long startTime, Long endTime,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog);

  AppdynamicsTier getAppdynamicsTier(AppDynamicsConfig appDynamicsConfig, long appdynamicsAppId, long tierId,
      List<EncryptedDataDetail> encryptionDetails, ThirdPartyApiCallLog apiCallLog);

  @DelegateTaskType(TaskType.APPDYNAMICS_METRIC_DATA_FOR_NODE)
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(AppDynamicsConfig appDynamicsConfig,
      List<EncryptedDataDetail> encryptionDetails, AppdynamicsSetupTestNodeData setupTestNodeData, String hostName,
      ThirdPartyApiCallLog apiCallLog);

  @DelegateTaskType(TaskType.APPDYNAMICS_METRIC_PACK_DATA)
  Set<AppdynamicsValidationResponse> getMetricPackData(AppDynamicsConfig appDynamicsConfig,
      List<EncryptedDataDetail> encryptionDetails, long appdAppId, long appdTierId, String requestGuid,
      List<MetricPack> metricPacks, Instant startTime, Instant endTime);
}
