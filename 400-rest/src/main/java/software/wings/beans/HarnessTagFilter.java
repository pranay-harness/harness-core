/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.beans.SearchFilter.Operator;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "HarnessTagFilterKeys")
public class HarnessTagFilter {
  private boolean matchAll;
  private List<TagFilterCondition> conditions;

  @Data
  @Builder
  public static class TagFilterCondition {
    private String name;
    private HarnessTagType tagType;
    private List<String> values;
    Operator operator;
  }
}
