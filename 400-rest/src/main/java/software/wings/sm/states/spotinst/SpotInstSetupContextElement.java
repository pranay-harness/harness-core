package software.wings.sm.states.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.spotinst.model.ElastiGroup;

import software.wings.beans.ResizeStrategy;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@JsonTypeName("spotInstSetupContextElement")
@OwnedBy(CDP)
public class SpotInstSetupContextElement implements ContextElement, SweepingOutput {
  private String uuid;
  private String name;
  private String commandName;
  private Integer maxInstanceCount;
  private boolean useCurrentRunningInstanceCount;
  private Integer currentRunningInstanceCount;
  private ResizeStrategy resizeStrategy;
  private boolean isBlueGreen;
  private String appId;
  private String envId;
  private String serviceId;
  private String infraMappingId;
  private SpotInstCommandRequest commandRequest;
  private SpotInstSetupTaskResponse spotInstSetupTaskResponse;
  private ElastiGroup newElastiGroupOriginalConfig;
  private ElastiGroup oldElastiGroupOriginalConfig;
  private List<LoadBalancerDetailsForBGDeployment> lbDetailsForBGDeployment;
  private String elstiGroupNamePrefix;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.SPOTINST_SERVICE_SETUP;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }

  @Override
  public String getType() {
    return "spotInstSetupContextElement";
  }
}
