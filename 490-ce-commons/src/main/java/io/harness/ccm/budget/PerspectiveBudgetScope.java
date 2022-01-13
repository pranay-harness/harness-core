/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.budget;

import static io.harness.ccm.budget.BudgetScopeType.PERSPECTIVE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@JsonTypeName("PERSPECTIVE")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "PerspectiveBudgetScopeKeys")
public class PerspectiveBudgetScope implements BudgetScope {
  String viewId;
  String viewName;

  @Override
  public String getBudgetScopeType() {
    return PERSPECTIVE;
  }

  @Override
  public List<String> getEntityIds() {
    return Collections.singletonList(viewId);
  }

  @Override
  public List<String> getEntityNames() {
    return Collections.singletonList(viewName);
  }
}
