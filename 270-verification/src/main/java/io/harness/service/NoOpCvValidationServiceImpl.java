/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
