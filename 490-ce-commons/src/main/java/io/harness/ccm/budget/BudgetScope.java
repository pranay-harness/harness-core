/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.budget;

import static io.harness.ccm.budget.BudgetScopeType.APPLICATION;
import static io.harness.ccm.budget.BudgetScopeType.CLUSTER;
import static io.harness.ccm.budget.BudgetScopeType.PERSPECTIVE;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ApplicationBudgetScope.class, name = APPLICATION)
  , @JsonSubTypes.Type(value = ClusterBudgetScope.class, name = CLUSTER),
      @JsonSubTypes.Type(value = PerspectiveBudgetScope.class, name = PERSPECTIVE)
})
@Schema(description = "The scope in which the Budget was created")
public interface BudgetScope {
  String getBudgetScopeType();
  List<String> getEntityIds();
  List<String> getEntityNames();
}
