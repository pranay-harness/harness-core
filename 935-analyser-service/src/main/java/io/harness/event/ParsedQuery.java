package io.harness.event;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParsedQuery extends LinkedHashMap<String, Object> {
  public ParsedQuery() {}

  public ParsedQuery(Map<String, Object> objectMap) {
    super(objectMap);
  }
}
