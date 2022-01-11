/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
public class TemplateDeleteEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private TemplateEntity templateEntity;
  private String comments;

  public TemplateDeleteEvent(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      TemplateEntity templateEntity, String comments) {
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.templateEntity = templateEntity;
    this.comments = comments;
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
    return TemplateOutboxEvents.TEMPLATE_VERSION_DELETED;
  }
}
