/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
