/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.SECRET;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.secrets.SecretDTOV2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class SecretUpdateEvent implements Event {
  private SecretDTOV2 newSecret;
  private SecretDTOV2 oldSecret;
  private String accountIdentifier;

  public SecretUpdateEvent(String accountIdentifier, SecretDTOV2 newSecret, SecretDTOV2 oldSecret) {
    this.newSecret = newSecret;
    this.oldSecret = oldSecret;
    this.accountIdentifier = accountIdentifier;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isNotEmpty(newSecret.getOrgIdentifier())) {
      if (isEmpty(newSecret.getProjectIdentifier())) {
        return new OrgScope(accountIdentifier, newSecret.getOrgIdentifier());
      } else {
        return new ProjectScope(accountIdentifier, newSecret.getOrgIdentifier(), newSecret.getProjectIdentifier());
      }
    }
    return new AccountScope(accountIdentifier);
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newSecret.getName());
    return Resource.builder().identifier(newSecret.getIdentifier()).type(SECRET).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "SecretUpdated";
  }
}
