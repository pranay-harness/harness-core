package software.wings.beans.container;

import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.DeploymentSpecification;
import software.wings.common.Constants;

import javax.validation.constraints.NotNull;

@Entity("ecsServiceSpecification")
@HarnessExportableEntity
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class EcsServiceSpecification extends DeploymentSpecification {
  @NotNull @NaturalKey private String serviceId;
  private String serviceSpecJson;
  private String schedulingStrategy;

  public static final String preamble = "# Enter your Service JSON spec below.\n"
      + "# ---\n\n";

  public static final String manifestTemplate = "{\n\"placementConstraints\":[ ],\n"
      + "\"placementStrategy\":[ ],\n"
      + "\"healthCheckGracePeriodSeconds\":null,\n"
      + "\"tags\":[],\n"
      + "\"schedulingStrategy\":\"REPLICA\"\n}";

  public void resetToDefaultSpecification() {
    this.serviceSpecJson = manifestTemplate;
  }

  public EcsServiceSpecification cloneInternal() {
    EcsServiceSpecification specification = EcsServiceSpecification.builder()
                                                .serviceId(this.serviceId)
                                                .serviceSpecJson(serviceSpecJson)
                                                .schedulingStrategy(schedulingStrategy)
                                                .build();
    specification.setAppId(this.getAppId());
    return specification;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends DeploymentSpecification.Yaml {
    private String serviceSpecJson;
    private String schedulingStrategy = Constants.ECS_REPLICA_SCHEDULING_STRATEGY;

    @Builder
    public Yaml(String type, String harnessApiVersion, String serviceName, String manifestYaml, String serviceSpecJson,
        String schedulingStrategy) {
      super(type, harnessApiVersion);
      this.schedulingStrategy = schedulingStrategy;
      this.serviceSpecJson = serviceSpecJson;
    }
  }
}
