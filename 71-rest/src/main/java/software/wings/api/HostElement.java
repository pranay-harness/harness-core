/**
 *
 */

package software.wings.api;

import com.amazonaws.services.ec2.model.Instance;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Data;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class HostElement.
 *
 * @author Rishi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class HostElement implements ContextElement {
  private String uuid;
  private String hostName;
  private String ip;
  private String instanceId;
  private String publicDns;
  private Map<String, Object> properties;
  private Instance ec2Instance;
  private PcfInstanceElement pcfElement;

  @Override
  public String getName() {
    return hostName;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.HOST;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(ContextElement.HOST, this);
    return map;
  }

  @Override
  public ContextElement cloneMin() {
    return HostElement.builder()
        .uuid(uuid)
        .hostName(hostName)
        .ip(ip)
        .publicDns(publicDns)
        .instanceId(instanceId)
        //        .withEc2Instance(ec2Instance)
        .build();
  }
}
