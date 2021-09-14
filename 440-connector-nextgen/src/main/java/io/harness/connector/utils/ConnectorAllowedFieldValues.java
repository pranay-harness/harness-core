/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
                  Arrays.asList("https://api.us2.sumologic.com/", "https://api.sumologic.com/",
                      "https://api.in.sumologic.com/", "https://api.jp.sumologic.com/",
                      "https://api.fed.sumologic.com/", "https://api.eu.sumologic.com/",
                      "https://api.de.sumologic.com/", "https://api.ca.sumologic.com/",
                      "https://api.au.sumologic.com/"))
              .build());
    }
  };
}
