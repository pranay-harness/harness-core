package software.wings.api;

import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * The Class ServiceElement.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class ServiceElement implements ContextElement {
  private String uuid;
  private String name;
  private String description;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.SERVICE;
  }

  @Override
  public ContextElement cloneMin() {
    return ServiceElement.builder().uuid(uuid).name(name).description(description).build();
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(SERVICE, this);
    return map;
  }
}
