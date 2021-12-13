package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceType;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.service.entity.ServiceEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(PIPELINE)
@Getter
@Builder
@AllArgsConstructor
public class ServiceCreateEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private ServiceEntity service;

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new ProjectScope(accountIdentifier, service.getOrgIdentifier(), service.getProjectIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder().identifier(service.getIdentifier()).type(ResourceType.SERVICE.name()).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return OutboxEventConstants.SERVICE_CREATED;
  }
}
