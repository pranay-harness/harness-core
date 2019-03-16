package io.harness.service;

import software.wings.service.impl.verification.CvValidationService;

/**
 * Created by Pranjal on 03/14/2019
 */
public class NoOpCvValidationServiceImpl implements CvValidationService {
  @Override
  public Boolean validateELKQuery(String accountId, String appId, String settingId, String query, String index) {
    throw new UnsupportedOperationException();
  }
}
