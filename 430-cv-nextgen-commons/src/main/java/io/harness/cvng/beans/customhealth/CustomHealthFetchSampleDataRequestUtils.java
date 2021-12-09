package io.harness.cvng.beans.customhealth;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthMethod;
import io.harness.serializer.JsonUtils;

import java.util.HashMap;
import java.util.Map;

public class CustomHealthFetchSampleDataRequestUtils {
  public static Map<String, Object> getDSLEnvironmentVariables(String urlPath,
      Map<String, String> requestTimestampPlaceholderAndValues, CustomHealthMethod method, String body) {
    Map<String, Object> commonEnvVariables = new HashMap<>();
    commonEnvVariables.put("method", method);

    String pathWithReplacedPlaceholders = isNotEmpty(urlPath) ? urlPath : "";
    String bodyWithReplacedPlaceholders = isNotEmpty(body) ? body : "";

    for (Map.Entry<String, String> entry : requestTimestampPlaceholderAndValues.entrySet()) {
      String placeholder = entry.getKey();
      String value = entry.getValue();
      pathWithReplacedPlaceholders = pathWithReplacedPlaceholders.replaceFirst(placeholder, value);
      bodyWithReplacedPlaceholders = bodyWithReplacedPlaceholders.replaceFirst(placeholder, value);
    }

    commonEnvVariables.put("urlPath", pathWithReplacedPlaceholders);
    commonEnvVariables.put(
        "body", bodyWithReplacedPlaceholders.equals("") ? null : JsonUtils.asMap(bodyWithReplacedPlaceholders));
    return commonEnvVariables;
  }
}
