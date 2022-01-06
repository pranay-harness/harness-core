/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.cvng.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.pagerduty.PagerDutyConnectorDTO;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
@OwnedBy(CV)
public class PagerDutyUtils {
  private static final String PAGER_DUTY_URL = "https://api.pagerduty.com/";

  public static String getBaseUrl() {
    return PAGER_DUTY_URL;
  }

  public static String getAuthorizationHeader(PagerDutyConnectorDTO pagerDutyConnectorDTO) {
    return "Token token=" + new String(pagerDutyConnectorDTO.getApiTokenRef().getDecryptedValue());
  }

  public static Map<String, String> getCollectionHeaders(PagerDutyConnectorDTO pagerDutyConnectorDTO) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Connection", "keep-alive");
    headers.put("Authorization", getAuthorizationHeader(pagerDutyConnectorDTO));
    return headers;
  }
}
