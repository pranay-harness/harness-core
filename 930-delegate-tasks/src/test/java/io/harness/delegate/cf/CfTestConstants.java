/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.cf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.pcf.CfInternalConfig;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.request.CfCommandRouteUpdateRequest;

@OwnedBy(CDP)
public interface CfTestConstants {
  String URL = "URL";
  String ORG = "ORG";
  String SPACE = "SPACE";
  String ACCOUNT_ID = "ACCOUNT_ID";
  String RUNNING = "RUNNING";
  String APP_NAME = "APP_NAME";
  char[] USER_NAME_DECRYPTED = "USER_NAME_DECRYPTED".toCharArray();
  String APP_ID = "APP_ID";
  String ACTIVITY_ID = "ACTIVITY_ID";
  String NOT_MANIFEST_YML_ELEMENT = "NOT_MANIFEST_YML_ELEMENT";
  String CF_PATH = "cf-path/cf";
  String STOPPED = "STOPPED";
  String RELEASE_NAME = "name"
      + "_pcfCommandHelperTest";

  static CfInternalConfig getPcfConfig() {
    return CfInternalConfig.builder().username(USER_NAME_DECRYPTED).endpointUrl(URL).password(new char[0]).build();
  }

  static CfCommandRouteUpdateRequest getRouteUpdateRequest(
      CfRouteUpdateRequestConfigData routeUpdateRequestConfigData) {
    return CfCommandRouteUpdateRequest.builder()
        .pcfCommandType(CfCommandRequest.PcfCommandType.RESIZE)
        .pcfConfig(getPcfConfig())
        .accountId(ACCOUNT_ID)
        .organization(ORG)
        .space(SPACE)
        .timeoutIntervalInMin(2)
        .pcfCommandType(CfCommandRequest.PcfCommandType.UPDATE_ROUTE)
        .pcfRouteUpdateConfigData(routeUpdateRequestConfigData)
        .build();
  }
}
