package io.harness.ng.core.mapper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.ng.core.common.beans.KeyValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

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
