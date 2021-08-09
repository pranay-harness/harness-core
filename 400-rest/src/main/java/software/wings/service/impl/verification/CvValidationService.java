package software.wings.service.impl.verification;

/**
 * Created by Pranjal on 03/14/2019
 */
public interface CvValidationService {
  Boolean validateELKQuery(String accountId, String appId, String settingId, String query, String index,
      String hostnameField, String messageField, String timestampField);

  Boolean validateStackdriverQuery(
      String accountId, String appId, String connectorId, String query, String hostNameField, String logMessageField);
}
