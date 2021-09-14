/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.governance.pipeline.enforce;

import io.harness.data.structure.CollectionUtils;
import io.harness.governance.pipeline.service.model.MatchType;
import io.harness.governance.pipeline.service.model.Tag;

import software.wings.features.api.Usage;

import java.util.List;
import lombok.Value;

@Value
public class GovernanceRuleStatus {
  private List<Tag> tags;
  private int weight;
  private boolean tagsIncluded;
  private MatchType matchType;
  private List<Usage> tagsLocations;

  public List<Tag> getTags() {
    return CollectionUtils.emptyIfNull(tags);
  }

  public List<Usage> getTagsLocations() {
    return CollectionUtils.emptyIfNull(tagsLocations);
  }
}
