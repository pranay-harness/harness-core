package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.json.JSONObject;
import software.wings.APMFetchConfig;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.analysis.APMVerificationService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Misc;

/**
 * @author Praveen 9/6/18
 */

public class APMVerificationServiceImpl implements APMVerificationService {
  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      String accountId, String serverConfigId, APMFetchConfig fetchConfig) {
    try {
      if (isEmpty(serverConfigId) || fetchConfig == null) {
        throw new WingsException("Invalid Parameters passed while trying to get test data for APM");
      }
      SettingAttribute settingAttribute = settingsService.get(serverConfigId);
      APMVerificationConfig apmVerificationConfig = (APMVerificationConfig) settingAttribute.getValue();
      APMValidateCollectorConfig apmValidateCollectorConfig =
          APMValidateCollectorConfig.builder()
              .baseUrl(apmVerificationConfig.getUrl())
              .headers(apmVerificationConfig.collectionHeaders())
              .options(apmVerificationConfig.collectionParams())
              .url(fetchConfig.getUrl())
              .body(fetchConfig.getBody())
              .encryptedDataDetails(apmVerificationConfig.encryptedDataDetails(secretManager))
              .build();
      SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
      String apmResponse =
          delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).fetch(apmValidateCollectorConfig);
      // if response is an empty JSON, we will tag it as no-load.
      JSONObject jsonObject = new JSONObject(apmResponse);
      boolean hasLoad = false;
      if (jsonObject.length() != 0) {
        hasLoad = true;
      }
      VerificationLoadResponse loadResponse =
          VerificationLoadResponse.builder().loadResponse(apmResponse).isLoadPresent(hasLoad).build();
      return VerificationNodeDataSetupResponse.builder().loadResponse(loadResponse).dataForNode(apmResponse).build();

    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? Misc.getMessage(e.getCause()) : Misc.getMessage(e);
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR, USER).addParam("reason", errorMsg);
    }
  }
}
