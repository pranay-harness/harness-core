package io.harness.service;

import software.wings.service.impl.verification.CvValidationService;

/**
 * Created by Pranjal on 03/14/2019
 */
public class NoOpCvValidationServiceImpl implements CvValidationService {
  @Override
  public Boolean validateELKQuery(String accountId, String appId, String settingId, String query, String index,
      String hostnameField, String messageField, String timestampField) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean validateStackdriverQuery(
      String accountId, String appId, String connectorId, String query, String hostNameField, String logMessageField) {
    throw new UnsupportedOperationException();
  }
}
