/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server.beans;

import static org.apache.commons.collections4.SetUtils.emptyIfNull;

import io.harness.commandlibrary.server.utils.JsonSerializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.elasticsearch.common.util.set.Sets;

@Value
@FieldNameConstants(innerTypeName = "TagConfigKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagConfig implements JsonSerializable {
  Set<String> allowedTags;
  Set<String> importantTags;

  @Builder
  public TagConfig(@JsonProperty(TagConfigKeys.allowedTags) Set<String> allowedTags,
      @JsonProperty(TagConfigKeys.importantTags) Set<String> importantTags) {
    this.importantTags = emptyIfNull(importantTags);
    this.allowedTags = Sets.union(emptyIfNull(allowedTags), emptyIfNull(importantTags));
  }
}
