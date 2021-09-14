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

public class FeatureRestrictions extends AbstractMap<String, Map<String, Restrictions>> {
  @NonNull private final Map<String, Map<String, Restrictions>> restrictionsByFeature;

  @JsonCreator
  public FeatureRestrictions(@NonNull Map<String, Map<String, Restrictions>> restrictionsByFeature) {
    this.restrictionsByFeature = Collections.unmodifiableMap(restrictionsByFeature);
  }

  public boolean isRestrictedFeature(String featureName) {
    return restrictionsByFeature.containsKey(featureName);
  }

  public Map<String, Restrictions> getRestrictionsByAccountType(String featureName) {
    if (!isRestrictedFeature(featureName)) {
      throw new IllegalArgumentException(String.format("'%s' is not a restricted feature", featureName));
    }

    return restrictionsByFeature.get(featureName);
  }

  @Override
  public Set<Entry<String, Map<String, Restrictions>>> entrySet() {
    return restrictionsByFeature.entrySet();
  }
}
