/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.cvng.newrelic;

import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;

import java.util.HashMap;
import java.util.Map;

public class NewRelicUtils {
  private static final String ACCOUNTS_BASE_URL = "v1/accounts/";
  private static final String X_QUERY_KEY = "X-Query-Key";

  public static String getBaseUrl(NewRelicConnectorDTO newRelicConnectorDTO) {
    return newRelicConnectorDTO.getUrl() + ACCOUNTS_BASE_URL + newRelicConnectorDTO.getNewRelicAccountId() + "/";
  }

  public static Map<String, String> collectionHeaders(NewRelicConnectorDTO newRelicConnectorDTO) {
    String apiKey = new String(newRelicConnectorDTO.getApiKeyRef().getDecryptedValue());
    Map<String, String> headers = new HashMap<>();
    headers.put(X_QUERY_KEY, apiKey);
    headers.put("Accept", "application/json");
    return headers;
  }
}
