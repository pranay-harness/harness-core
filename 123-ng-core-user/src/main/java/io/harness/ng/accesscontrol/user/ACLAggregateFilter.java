/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.accesscontrol.user;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PL)
@ApiModel(value = "ACLAggregateFilter")
public class ACLAggregateFilter {
  Set<String> resourceGroupIdentifiers;
  Set<String> roleIdentifiers;

  public static boolean isFilterApplied(ACLAggregateFilter aclAggregateFilter) {
    return aclAggregateFilter != null
        && (!isEmpty(aclAggregateFilter.getRoleIdentifiers())
            || !isEmpty(aclAggregateFilter.getResourceGroupIdentifiers()));
  }
}
