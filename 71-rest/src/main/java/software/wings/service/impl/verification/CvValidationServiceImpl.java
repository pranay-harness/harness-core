package software.wings.service.impl.verification;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.stackdriver.StackDriverService;

/**
 * Created by Pranjal on 03/14/2019
 */
@Singleton
public class CvValidationServiceImpl implements CvValidationService {
  @Inject private ElkAnalysisService elkAnalysisService;
  @Inject private StackDriverService stackDriverService;

  @Override
  public Boolean validateELKQuery(String accountId, String appId, String settingId, String query, String index,
      String hostnameField, String messageField, String timestampField) {
    return elkAnalysisService.validateQuery(
        accountId, appId, settingId, query, index, null, hostnameField, messageField, timestampField);
  }

  @Override
  public Boolean validateStackdriverQuery(
      String accountId, String appId, String connectorId, String query, String hostNameField, String logMessageField) {
    return stackDriverService.validateQuery(accountId, appId, connectorId, query, hostNameField, logMessageField);
  }
}
