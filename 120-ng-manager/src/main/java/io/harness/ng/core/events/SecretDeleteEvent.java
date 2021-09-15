/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.secrets.SecretDTOV2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class SecretDeleteEvent implements Event {
  private SecretDTOV2 secret;
  private String accountIdentifier;

  public SecretDeleteEvent(String accountIdentifier, SecretDTOV2 secret) {
    this.secret = secret;
    this.accountIdentifier = accountIdentifier;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isNotEmpty(secret.getOrgIdentifier())) {
      if (isEmpty(secret.getProjectIdentifier())) {
        return new OrgScope(accountIdentifier, secret.getOrgIdentifier());
      } else {
        return new ProjectScope(accountIdentifier, secret.getOrgIdentifier(), secret.getProjectIdentifier());
      }
    }
    return new AccountScope(accountIdentifier);
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder().identifier(secret.getIdentifier()).type(SECRET).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "SecretDeleted";
  }
}
