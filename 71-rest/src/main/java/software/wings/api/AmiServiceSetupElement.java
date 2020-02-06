package software.wings.api;

import static com.google.common.collect.Maps.newHashMap;

import com.google.common.collect.ImmutableMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.context.ContextElementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.ResizeStrategy;
import software.wings.service.impl.aws.model.AwsAmiPreDeploymentData;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmiServiceSetupElement implements ContextElement {
  private String uuid;
  private String name;
  private String commandName;
  private int instanceCount;
  private String newAutoScalingGroupName;
  private String oldAutoScalingGroupName;
  private Integer autoScalingSteadyStateTimeout;
  private Integer maxInstances;
  private int desiredInstances;
  private int minInstances;
  private List<String> oldAsgNames;
  private AwsAmiPreDeploymentData preDeploymentData;
  private boolean blueGreen;
  private ResizeStrategy resizeStrategy;
  private List<String> baseScalingPolicyJSONs;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.AMI_SERVICE_SETUP;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = newHashMap();
    map.put("newAsgName", newAutoScalingGroupName);
    map.put("oldAsgName", oldAutoScalingGroupName);
    return ImmutableMap.of("ami", map);
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }
}
