package io.harness.ng.core.dto;

import io.harness.Event;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
public class EntityChangeEvent implements Event {
  @NotNull ResourceScope resourceScope;
  @NotNull @Valid Resource resource;
  @NotEmpty String action;

  @Override
  public ResourceScope getResourceScope() {
    return this.resourceScope;
  }

  @Override
  public Resource getResource() {
    return this.resource;
  }

  @Override
  public Object getEventData() {
    return this.action;
  }

  @Override
  public String getEventType() {
    return "EntityChange";
  }
}
