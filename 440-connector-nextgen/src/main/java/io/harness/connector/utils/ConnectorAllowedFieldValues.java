/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
  public static Map<ConnectorType, io.harness.connector.utils.FieldValues> TYPE_TO_FIELDS =
      new HashMap<ConnectorType, io.harness.connector.utils.FieldValues>() {
        {
          put(ConnectorType.NEW_RELIC,
              io.harness.connector.utils.FieldValues.builder()
                  .fieldValue("url",
                      Arrays.asList("https://insights-api.newrelic.com/", "https://insights-api.eu.newrelic.com/"))
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
