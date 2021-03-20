package software.wings.beans.container;

import io.harness.annotation.HarnessEntity;

import software.wings.beans.DeploymentSpecification;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "EcsServiceSpecificationKeys")
@Entity("ecsServiceSpecification")
@HarnessEntity(exportable = true)
public class EcsServiceSpecification extends DeploymentSpecification {
  public static final String ECS_REPLICA_SCHEDULING_STRATEGY = "REPLICA";
  @NotNull private String serviceId;
  private String serviceSpecJson;
  private String schedulingStrategy;

  public static final String preamble = "# Enter your Service JSON spec below.\n"
      + "# ---\n\n";

  public static final String manifestTemplate = "{\n\"placementConstraints\":[ ],\n"
      + "\"placementStrategy\":[ ],\n"
      + "\"healthCheckGracePeriodSeconds\":null,\n"
      + "\"tags\":[ ],\n"
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
    specification.setAccountId(this.getAccountId());
    specification.setAppId(this.getAppId());
    return specification;
  }
}
