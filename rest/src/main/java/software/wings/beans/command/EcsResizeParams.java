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

  public static final class EcsResizeParamsBuilder {
    private String region;
    private String clusterName;
    private List<ContainerServiceData> desiredCounts = new ArrayList<>();
    private int serviceSteadyStateTimeout;

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

    public EcsResizeParamsBuilder withDesiredCounts(List<ContainerServiceData> desiredCounts) {
      this.desiredCounts = desiredCounts;
      return this;
    }

    public EcsResizeParamsBuilder withServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
      this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
      return this;
    }

    public EcsResizeParamsBuilder but() {
      return anEcsResizeParams()
          .withRegion(region)
          .withClusterName(clusterName)
          .withDesiredCounts(desiredCounts)
          .withServiceSteadyStateTimeout(serviceSteadyStateTimeout);
    }

    public EcsResizeParams build() {
      EcsResizeParams ecsResizeParams = new EcsResizeParams();
      ecsResizeParams.setRegion(region);
      ecsResizeParams.setClusterName(clusterName);
      ecsResizeParams.setDesiredCounts(desiredCounts);
      ecsResizeParams.setServiceSteadyStateTimeout(serviceSteadyStateTimeout);
      return ecsResizeParams;
    }
  }
}
