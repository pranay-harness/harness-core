/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.cvng.dynatrace;

import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;

import java.util.HashMap;
import java.util.Map;

public class DynatraceUtils {
  private static final String DYNATRACE_API_TOKEN_LABEL = "Api-Token";

  public static Map<String, String> collectionHeaders(DynatraceConnectorDTO dynatraceConnectorDTO) {
    String apiToken = new String(dynatraceConnectorDTO.getApiTokenRef().getDecryptedValue());
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", String.format("%s %s", DYNATRACE_API_TOKEN_LABEL, apiToken));
    headers.put("Accept", "application/json");
    return headers;
  }
}
