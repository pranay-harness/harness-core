/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
