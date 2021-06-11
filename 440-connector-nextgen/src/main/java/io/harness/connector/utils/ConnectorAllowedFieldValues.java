package io.harness.connector.utils;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(CV)
public class ConnectorAllowedFieldValues {
  public static Map<ConnectorType, FieldValues> TYPE_TO_FIELDS = new HashMap<ConnectorType, FieldValues>() {
    {
      put(ConnectorType.NEW_RELIC,
          FieldValues.builder()
              .fieldValue(
                  "url", Arrays.asList("https://insights-api.newrelic.com/", "https://insights-api.eu.newrelic.com/"))
              .build());

      put(ConnectorType.SUMOLOGIC,
          FieldValues.builder()
              .fieldValue("url",
                  Arrays.asList("https://api.us2.sumologic.com/api/v1/", "https://api.sumologic.com/api/v1/",
                      "https://api.in.sumologic.com/api/v1/", "https://api.jp.sumologic.com/api/v1/",
                      "https://api.fed.sumologic.com/api/v1/", "https://api.eu.sumologic.com/api/v1/",
                      "https://api.de.sumologic.com/api/v1/", "https://api.ca.sumologic.com/api/v1/",
                      "https://api.au.sumologic.com/api/v1/"))
              .build());
    }
  };
}