package software.wings.beans.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ContainerServiceData;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class EcsResizeParams extends ContainerResizeParams {
  private String region;
  private int ecsServiceSteadyStateTimeout;

  public static final class EcsResizeParamsBuilder {
    private String region;
    private String clusterName;
    private int ecsServiceSteadyStateTimeout;
    private List<ContainerServiceData> desiredCounts = new ArrayList<>();

    private EcsResizeParamsBuilder() {}

    public static EcsResizeParamsBuilder anEcsResizeParams() {
      return new EcsResizeParamsBuilder();
    }

    public EcsResizeParamsBuilder withRegion(String region) {
      this.region = region;
      return this;
    }

    public EcsResizeParamsBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public EcsResizeParamsBuilder withEcsServiceSteadyStateTimeout(int ecsServiceSteadyStateTimeout) {
      this.ecsServiceSteadyStateTimeout = ecsServiceSteadyStateTimeout;
      return this;
    }

    public EcsResizeParamsBuilder withDesiredCounts(List<ContainerServiceData> desiredCounts) {
      this.desiredCounts = desiredCounts;
      return this;
    }

    public EcsResizeParamsBuilder but() {
      return anEcsResizeParams()
          .withRegion(region)
          .withClusterName(clusterName)
          .withEcsServiceSteadyStateTimeout(ecsServiceSteadyStateTimeout)
          .withDesiredCounts(desiredCounts);
    }

    public EcsResizeParams build() {
      EcsResizeParams ecsResizeParams = new EcsResizeParams();
      ecsResizeParams.setRegion(region);
      ecsResizeParams.setClusterName(clusterName);
      ecsResizeParams.setEcsServiceSteadyStateTimeout(ecsServiceSteadyStateTimeout);
      ecsResizeParams.setDesiredCounts(desiredCounts);
      return ecsResizeParams;
    }
  }
}
