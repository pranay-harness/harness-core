/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
