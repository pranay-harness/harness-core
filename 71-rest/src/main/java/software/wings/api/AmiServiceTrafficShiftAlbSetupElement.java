package software.wings.api;

import io.harness.context.ContextElementType;
import io.harness.data.SweepingOutput;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;

import software.wings.service.impl.aws.model.AwsAmiPreDeploymentData;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmiServiceTrafficShiftAlbSetupElement implements ContextElement, SweepingOutput {
  private String uuid;
  private String name;
  private String commandName;
  private int instanceCount;
  private String newAutoScalingGroupName;
  private String oldAutoScalingGroupName;
  private Integer autoScalingSteadyStateTimeout;
  private Integer maxInstances;
  private Integer desiredInstances;
  private Integer minInstances;
  private List<String> oldAsgNames;
  private AwsAmiPreDeploymentData preDeploymentData;
  private List<String> baseScalingPolicyJSONs;
  private List<LbDetailsForAlbTrafficShift> detailsWithTargetGroups;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.AMI_SERVICE_SETUP;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put("newAsgName", newAutoScalingGroupName);
    map.put("oldAsgName", oldAutoScalingGroupName);
    return ImmutableMap.of("ami", map);
  }
}
