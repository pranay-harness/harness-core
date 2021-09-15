/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.template.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceTypeConstants;
import io.harness.encryption.Scope;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.template.entity.TemplateEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Getter
@NoArgsConstructor
public class TemplateCreateEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private TemplateEntity templateEntity;

  public TemplateCreateEvent(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, TemplateEntity templateEntity) {
    this.orgIdentifier = orgIdentifier;
    this.accountIdentifier = accountIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.templateEntity = templateEntity;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (templateEntity.getTemplateScope().equals(Scope.PROJECT)) {
      return new ProjectScope(accountIdentifier, orgIdentifier, projectIdentifier);
    } else if (templateEntity.getTemplateScope().equals(Scope.ORG)) {
      return new OrgScope(accountIdentifier, orgIdentifier);
    } else {
      return new AccountScope(accountIdentifier);
    }
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder()
        .identifier(templateEntity.getIdentifier())
        .type(ResourceTypeConstants.TEMPLATE)
        .labels(ImmutableMap.<String, String>builder().put("versionLabel", templateEntity.getVersionLabel()).build())
        .build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return TemplateOutboxEvents.TEMPLATE_VERSION_CREATED;
  }
}
