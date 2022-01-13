/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
