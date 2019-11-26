package software.wings.service.impl.dynatrace;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.dynatrace.DynaTraceService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Pranjal on 09/12/2018
 */
@Singleton
@Slf4j
public class DynaTraceServiceImpl implements DynaTraceService {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(DynaTraceSetupTestNodeData setupTestNodeData) {
    try {
      final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
      setupTestNodeData.setFromTime(TimeUnit.SECONDS.toMillis(setupTestNodeData.getFromTime()));
      setupTestNodeData.setToTime(TimeUnit.SECONDS.toMillis(setupTestNodeData.getToTime()));
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      List<DynaTraceMetricDataResponse> response =
          delegateProxyFactory.get(DynaTraceDelegateService.class, syncTaskContext)
              .getMetricsWithDataForNode((DynaTraceConfig) settingAttribute.getValue(), encryptionDetails,
                  setupTestNodeData, createApiCallLog(settingAttribute.getAccountId(), setupTestNodeData.getGuid()));
      if (response.isEmpty()) {
        return VerificationNodeDataSetupResponse.builder()
            .providerReachable(true)
            .loadResponse(VerificationLoadResponse.builder().isLoadPresent(false).build())
            .build();
      } else {
        return VerificationNodeDataSetupResponse.builder()
            .providerReachable(true)
            .dataForNode(response)
            .loadResponse(VerificationLoadResponse.builder()
                              .isLoadPresent(isDataPresent(response))
                              .loadResponse(response)
                              .build())
            .build();
      }
    } catch (Exception e) {
      logger.info("error getting metric data for node", e);
      throw new VerificationOperationException(
          ErrorCode.DYNA_TRACE_ERROR, "Error in getting metric data for the node. " + e.getMessage());
    }
  }

  private boolean isDataPresent(List<DynaTraceMetricDataResponse> responses) {
    AtomicBoolean isDataPresent = new AtomicBoolean(false);
    responses.forEach(response -> {
      if (isNotEmpty(response.getResult().getDataPoints())) {
        isDataPresent.set(true);
        return;
      }
    });
    return isDataPresent.get();
  }
}
