package io.harness.input;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class InputSet {
  private final Map<String, Object> map = new HashMap<>();

  public Object get(String key) {
    validateKey(key);
    Pair<String, String> pair = splitKey(key);
    String currKey = pair.getLeft();
    String nextKey = pair.getRight();
    if (nextKey == null) {
      return map.get(currKey);
    }

    Object currValue = map.get(currKey);
    if (currValue == null) {
      return null;
    } else {
      if (!(currValue instanceof InputSet)) {
        throw new InvalidRequestException(format("Key '%s' contains a final value", currKey));
      }

      return ((InputSet) currValue).get(nextKey);
    }
  }

  private void put(String key, Object value) {
    validateKey(key);
    validateValue(value);
    Pair<String, String> pair = splitKey(key);
    String currKey = pair.getLeft();
    String nextKey = pair.getRight();
    if (nextKey == null) {
      map.put(currKey, value);
      return;
    }

    Object currValue = map.get(currKey);
    InputSet inputSet;
    if (currValue == null) {
      inputSet = new InputSet();
    } else {
      if (!(currValue instanceof InputSet)) {
        throw new InvalidRequestException(format("Key '%s' contains a final value", currKey));
      }

      inputSet = (InputSet) currValue;
    }

    inputSet.put(nextKey, value);
    map.put(currKey, inputSet);
  }

  private Pair<String, String> splitKey(String key) {
    int idx = key.indexOf('.');
    if (idx < 0) {
      return Pair.of(key, null);
    }

    return Pair.of(key.substring(0, idx), key.substring(idx + 1));
  }

  private void validateKey(String key) {
    if (EmptyPredicate.isEmpty(key) || key.charAt(0) == '.') {
      throw new InvalidRequestException("Key is empty");
    }
  }

  private void validateValue(Object value) {
    if (value == null) {
      throw new InvalidRequestException("Value is null");
    }
  }

  private void putAll(Map<String, Object> initialMap) {
    if (EmptyPredicate.isEmpty(initialMap)) {
      return;
    }

    for (Map.Entry<String, Object> entry : initialMap.entrySet()) {
      if (entry.getValue() == null) {
        continue;
      }

      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof Map) {
        InputSet child = new InputSet();
        child.putAll(((Map<Object, Object>) value)
                         .entrySet()
                         .stream()
                         .filter(e -> e.getKey() instanceof String && e.getValue() != null)
                         .collect(Collectors.toMap(e -> (String) e.getKey(), Map.Entry::getValue)));
        put(key, child);
      } else if (value instanceof InputSet) {
        InputSet child = new InputSet();
        child.putAll(((InputSet) value).map);
        put(key, child);
      } else {
        put(key, value);
      }
    }
  }

  public InputSet withOverrides(Map<String, Object> initialMap) {
    if (initialMap != null) {
      putAll(initialMap);
    }
    return this;
  }

  public InputSet withOverrides(InputSet inputSet) {
    if (inputSet != null) {
      putAll(inputSet.map);
    }
    return this;
  }
}
