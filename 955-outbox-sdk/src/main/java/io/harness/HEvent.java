package io.harness;

import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
public class HEvent implements Event {
  @NotNull ResourceScope resourceScope;
  @NotNull @Valid Resource resource;
  Object eventData;
  @NotEmpty String eventType;
}
