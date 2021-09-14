/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;

public class Restrictions extends AbstractMap<String, Object> {
  @NonNull private final Map<String, Object> properties;

  @JsonCreator
  public Restrictions(Map<String, Object> properties) {
    this.properties = Collections.unmodifiableMap(properties);
  }

  @JsonCreator
  public Restrictions() {
    this.properties = Collections.emptyMap();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return properties.entrySet();
  }
}
