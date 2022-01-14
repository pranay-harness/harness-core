package io.harness.ngtriggers.events;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceTypeConstants;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PIPELINE)
@Getter
@NoArgsConstructor
public class TriggerUpdateEvent implements Event {
  private String orgIdentifier;
  private String accountIdentifier;
  private String projectIdentifier;
  private NGTriggerEntity oldTriggerEntity;
  private NGTriggerEntity newTriggerEntity;
  public TriggerUpdateEvent(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      NGTriggerEntity oldTriggerEntity, NGTriggerEntity newTriggerEntity) {
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.oldTriggerEntity = oldTriggerEntity;
    this.newTriggerEntity = newTriggerEntity;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new ProjectScope(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder().identifier(newTriggerEntity.getIdentifier()).type(ResourceTypeConstants.TRIGGER).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return TriggerOutboxEvents.TRIGGER_UPDATED;
  }
}
