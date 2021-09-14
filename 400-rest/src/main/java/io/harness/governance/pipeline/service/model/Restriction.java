/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.governance.pipeline.service.model;

import io.harness.data.structure.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Restriction {
  public enum RestrictionType { APP_BASED, TAG_BASED }

  private RestrictionType type;
  private List<String> appIds;
  private List<Tag> tags;

  public List<String> getAppIds() {
    return CollectionUtils.emptyIfNull(appIds);
  }

  public List<Tag> getTags() {
    return CollectionUtils.emptyIfNull(tags);
  }
}
