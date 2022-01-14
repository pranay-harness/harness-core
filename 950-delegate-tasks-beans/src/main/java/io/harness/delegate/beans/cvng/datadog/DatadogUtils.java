package io.harness.delegate.beans.cvng.datadog;

import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;

import java.util.HashMap;
import java.util.Map;

public class DatadogUtils {
  private static final String DD_API_KEY = "DD-API-KEY";
  private static final String DD_APPLICATION_KEY = "DD-APPLICATION-KEY";

  private DatadogUtils() {}

  public static Map<String, String> collectionHeaders(DatadogConnectorDTO datadogConnectorDTO) {
    String apiKey = new String(datadogConnectorDTO.getApiKeyRef().getDecryptedValue());
    String appKey = new String(datadogConnectorDTO.getApplicationKeyRef().getDecryptedValue());
    Map<String, String> headers = new HashMap<>();
    headers.put(DD_API_KEY, apiKey);
    headers.put(DD_APPLICATION_KEY, appKey);
    headers.put("Accept", "application/json");
    return headers;
  }
}
