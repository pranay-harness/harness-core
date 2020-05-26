package software.wings.api;

import io.harness.beans.SweepingOutput;
import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 4/11/17.
 */
@Data
@Builder
public class ContainerRollbackRequestElement implements ContextElement, SweepingOutput {
  public static final String CONTAINER_ROLLBACK_REQUEST_PARAM = "CONTAINER_ROLLBACK_REQUEST_PARAM";

  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;
  private String namespace;
  private String controllerNamePrefix;
  private String previousEcsServiceSnapshotJson;
  private List<AwsAutoScalarConfig> previousAwsAutoScalarConfigs;
  private String ecsServiceArn;
  private String releaseName;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return CONTAINER_ROLLBACK_REQUEST_PARAM;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }
}
