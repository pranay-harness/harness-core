/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.verification;

import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.stackdriver.StackDriverService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
