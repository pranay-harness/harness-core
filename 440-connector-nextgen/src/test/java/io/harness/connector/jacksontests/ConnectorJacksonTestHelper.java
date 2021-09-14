/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.jacksontests;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.junit.Assert;

@UtilityClass
public class ConnectorJacksonTestHelper {
  static String connectorIdentifier = "identifier";
  static String name = "name";
  static String description = "description";
  static String projectIdentifier = "projectIdentifier";
  static String orgIdentifier = "orgIdentifier";

  public static String readFileAsString(String file) {
    try {
      return new String(Files.readAllBytes(Paths.get(file)));
    } catch (Exception ex) {
      Assert.fail("Failed reading the json from " + file + " with error " + ex.getMessage());
      return "";
    }
  }

  public static ConnectorInfoDTO createConnectorRequestDTO(
      ConnectorConfigDTO connectorConfigDTO, ConnectorType connectorType) {
    Map<String, String> tags = new HashMap<String, String>() {
      {
        put("company", "Harness");
        put("env", "dev");
      }
    };
    return ConnectorInfoDTO.builder()
        .name(name)
        .identifier(connectorIdentifier)
        .description(description)
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .tags(tags)
        .connectorType(connectorType)
        .connectorConfig(connectorConfigDTO)
        .build();
  }
}
