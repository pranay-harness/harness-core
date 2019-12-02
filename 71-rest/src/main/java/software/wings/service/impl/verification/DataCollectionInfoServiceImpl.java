package software.wings.service.impl.verification;

import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SplunkConfig;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfoV2;
import software.wings.service.impl.splunk.SplunkDataCollectionInfoV2;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.verification.DataCollectionInfoService;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.SplunkCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DataCollectionInfoServiceImpl implements DataCollectionInfoService {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public DataCollectionInfoV2 create(CVConfiguration config, Instant startTime, Instant endTime) {
    DataCollectionInfoV2 dataCollectionInfo = null;
    // TODO: keeping switch cases for now but We need to move this to specific implementation of each types so that this
    // method is not aware of the specific types and each conversion can be tested independently
    switch (config.getStateType()) {
      case SPLUNKV2:
        SplunkCVConfiguration splunkCVConfiguration = (SplunkCVConfiguration) config;
        SplunkConfig splunkConfig = (SplunkConfig) settingsService.get(config.getConnectorId()).getValue();
        dataCollectionInfo =
            SplunkDataCollectionInfoV2.builder()
                .splunkConfig(splunkConfig)
                .accountId(splunkConfig.getAccountId())
                .applicationId(config.getAppId())
                .startTime(startTime)
                .endTime(endTime)
                .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getUuid())
                .serviceId(config.getServiceId())
                .query(splunkCVConfiguration.getQuery())
                .hostnameField(splunkCVConfiguration.getHostnameField())
                .hosts(Collections.emptySet())
                .cvConfigId(config.getUuid())
                .encryptedDataDetails(secretManager.getEncryptionDetails(splunkConfig, config.getAppId(), null))
                .build();
        break;
      case NEW_RELIC:
        NewRelicCVServiceConfiguration newRelicCVServiceConfiguration = (NewRelicCVServiceConfiguration) config;
        Map<String, String> hostsMap = new HashMap<>();
        hostsMap.put("DUMMY_24_7_HOST", DEFAULT_GROUP_NAME);
        NewRelicConfig newRelicConfig = (NewRelicConfig) settingsService.get(config.getConnectorId()).getValue();
        dataCollectionInfo =
            NewRelicDataCollectionInfoV2.builder()
                .newRelicConfig(newRelicConfig)
                .accountId(config.getAccountId())
                .applicationId(config.getAppId())
                .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getUuid())
                .serviceId(config.getServiceId())
                .startTime(startTime)
                .endTime(endTime)
                .cvConfigId(config.getUuid())
                .newRelicAppId(Long.parseLong(newRelicCVServiceConfiguration.getApplicationId()))
                .hostsToGroupNameMap(hostsMap)
                .encryptedDataDetails(secretManager.getEncryptionDetails(newRelicConfig, config.getAppId(), null))
                .build();
        break;
      default:
        logger.error("Calling collect 24x7 data for an unsupported state : {}", config.getStateType());
        throw new RuntimeException("Not supported");
    }
    return dataCollectionInfo;
  }
}
