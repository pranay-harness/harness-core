package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceType;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.environment.beans.Environment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(PIPELINE)
@Getter
@Builder
@AllArgsConstructor
public class EnvironmentCreateEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private Environment environment;

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new ProjectScope(accountIdentifier, environment.getOrgIdentifier(), environment.getProjectIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder().identifier(environment.getIdentifier()).type(ResourceType.ENVIRONMENT.name()).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return OutboxEventConstants.ENVIRONMENT_CREATED;
  }
}
