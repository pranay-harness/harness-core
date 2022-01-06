/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
