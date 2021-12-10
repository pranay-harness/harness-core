package io.harness.template.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceTypeEnum;
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
public class TemplateUpdateEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private TemplateEntity newTemplateEntity;
  private TemplateEntity oldTemplateEntity;
  private String comments;
  private TemplateUpdateEventType templateUpdateEventType;

  public TemplateUpdateEvent(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      TemplateEntity newTemplateEntity, TemplateEntity oldTemplateEntity, String comments,
      TemplateUpdateEventType templateUpdateEventType) {
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.newTemplateEntity = newTemplateEntity;
    this.oldTemplateEntity = oldTemplateEntity;
    this.comments = comments;
    this.templateUpdateEventType = templateUpdateEventType;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (newTemplateEntity.getTemplateScope().equals(Scope.PROJECT)) {
      return new ProjectScope(accountIdentifier, orgIdentifier, projectIdentifier);
    } else if (newTemplateEntity.getTemplateScope().equals(Scope.ORG)) {
      return new OrgScope(accountIdentifier, orgIdentifier);
    } else {
      return new AccountScope(accountIdentifier);
    }
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder()
        .identifier(newTemplateEntity.getIdentifier())
        .type(Resource.Type.TEMPLATE)
        .labels(ImmutableMap.<String, String>builder().put("versionLabel", newTemplateEntity.getVersionLabel()).build())
        .build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return TemplateOutboxEvents.TEMPLATE_VERSION_UPDATED;
  }
}
