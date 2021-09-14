/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.KeyValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class KeyValuePairMapper {
  public static List<KeyValuePair> convertToList(Map<String, String> keyValuePair) {
    List<KeyValuePair> keyValuePairList = new ArrayList<>();
    if (isEmpty(keyValuePair)) {
      return keyValuePairList;
    }
    keyValuePair.forEach((key, value) -> keyValuePairList.add(KeyValuePair.builder().key(key).value(value).build()));
    return keyValuePairList;
  }

  public static Map<String, String> convertToMap(List<KeyValuePair> keyValuePairs) {
    Map<String, String> keyValuePairMap = new HashMap<>();
    if (isEmpty(keyValuePairs)) {
      return keyValuePairMap;
    }
    keyValuePairs.forEach(keyValuePair -> keyValuePairMap.put(keyValuePair.getKey(), keyValuePair.getValue()));
    return keyValuePairMap;
  }
}
