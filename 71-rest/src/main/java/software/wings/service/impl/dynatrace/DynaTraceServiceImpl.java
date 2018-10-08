package software.wings.service.impl.dynatrace;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.dynatrace.DynaTraceService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;

/**
 * Created by Pranjal on 09/12/2018
 */
public class DynaTraceServiceImpl implements DynaTraceService {
  private static final Logger logger = LoggerFactory.getLogger(DynaTraceServiceImpl.class);
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(DynaTraceSetupTestNodeData setupTestNodeData) {
    try {
      final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = aContext()
                                            .withAccountId(settingAttribute.getAccountId())
                                            .withAppId(Base.GLOBAL_APP_ID)
                                            .withTimeout(Constants.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      List<DynaTraceMetricDataResponse> response =
          delegateProxyFactory.get(DynaTraceDelegateService.class, syncTaskContext)
              .getMetricsWithDataForNode((DynaTraceConfig) settingAttribute.getValue(), encryptionDetails,
                  setupTestNodeData, apiCallLogWithDummyStateExecution(settingAttribute.getAccountId()));
      if (response.isEmpty()) {
        return VerificationNodeDataSetupResponse.builder()
            .providerReachable(true)
            .loadResponse(VerificationLoadResponse.builder().isLoadPresent(false).build())
            .build();
      } else {
        return VerificationNodeDataSetupResponse.builder()
            .providerReachable(true)
            .dataForNode(response)
            .loadResponse(VerificationLoadResponse.builder().isLoadPresent(true).build())
            .build();
      }
    } catch (Exception e) {
      logger.info("error getting metric data for node", e);
      throw new WingsException(ErrorCode.DYNA_TRACE_ERROR)
          .addParam("message", "Error in getting metric data for the node. " + e.getMessage());
    }
  }
}
