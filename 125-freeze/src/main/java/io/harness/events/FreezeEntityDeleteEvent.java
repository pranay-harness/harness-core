/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.audit.ResourceTypeConstants.DEPLOYMENT_FREEZE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.freeze.entity.FreezeConfigEntity;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.codehaus.jackson.annotate.JsonIgnore;

@OwnedBy(CDC)
@Getter
@Builder
@AllArgsConstructor
public class FreezeEntityDeleteEvent implements Event {
  public static final String DEPLOYMENT_FREEZE_DELETED = "DeploymentFreezeDeleted";
  private String accountIdentifier;
  private FreezeConfigEntity deletedFreeze;

  @Override
  @JsonIgnore
  public ResourceScope getResourceScope() {
    if (isNotEmpty(deletedFreeze.getOrgIdentifier())) {
      if (isEmpty(deletedFreeze.getProjectIdentifier())) {
        return new OrgScope(accountIdentifier, deletedFreeze.getOrgIdentifier());
      } else {
        return new ProjectScope(
            accountIdentifier, deletedFreeze.getOrgIdentifier(), deletedFreeze.getProjectIdentifier());
      }
    }
    return new AccountScope(accountIdentifier);
  }

  @Override
  @JsonIgnore
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, deletedFreeze.getName());
    return Resource.builder().identifier(deletedFreeze.getIdentifier()).type(DEPLOYMENT_FREEZE).labels(labels).build();
  }

  @Override
  @JsonIgnore
  public String getEventType() {
    return DEPLOYMENT_FREEZE_DELETED;
  }
}
