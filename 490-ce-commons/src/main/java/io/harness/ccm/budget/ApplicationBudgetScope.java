/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.budget;

import static io.harness.ccm.budget.BudgetScopeType.APPLICATION;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@JsonTypeName("APPLICATION")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ApplicationBudgetScopeKeys")
public class ApplicationBudgetScope implements BudgetScope {
  String[] applicationIds;
  EnvironmentType environmentType;

  @Override
  public String getBudgetScopeType() {
    return APPLICATION;
  }

  @Override
  public List<String> getEntityIds() {
    return Arrays.asList(applicationIds);
  }

  @Override
  public List<String> getEntityNames() {
    return null;
  }
}
