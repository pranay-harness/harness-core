/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.opaclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class OpaUtils {
  private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public static Object extractObjectFromYamlString(String yaml, String key) throws IOException {
    if (EmptyPredicate.isEmpty(yaml)) {
      return null;
    }
    Map<String, Object> map = (Map<String, Object>) objectMapper.readValue(yaml, Map.class);
    if (EmptyPredicate.isEmpty(key)) {
      return map;
    } else {
      return map.get(key);
    }
  }
}
