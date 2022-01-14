package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.LinkedHashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class ParsedQuery extends LinkedHashMap<String, Object> {
  public ParsedQuery() {}

  public ParsedQuery(Map<String, Object> objectMap) {
    super(objectMap);
  }
}
