/**
 *
 */

package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * This class holds information for verification step.
 * any instance has unique application_Guid + InstanceIndex combination, by which it can be accessed.
 * e.g. curl myapp.private-domain.example.com -H "X-Cf-App-Instance: 5cdc7595-2e9b-4f62-8d5a-a86b92f2df0e:9"
 * where 5cdc7595-2e9b-4f62-8d5a-a86b92f2df0e is appGuid and 9 is instanceIndex
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcfInstanceElement implements ContextElement {
  private String uuid;
  private String applicationId;
  private String instanceIndex;
  private String displayName;
  private boolean isUpsize;

  @Override
  public String getName() {
    return displayName;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PCF_INSTANCE;
  }

  @Override
  public String getUuid() {
    return null;
  }

  // @TODO why needed ?
  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(PCF_INSTANCE, this);
    return map;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }
}
