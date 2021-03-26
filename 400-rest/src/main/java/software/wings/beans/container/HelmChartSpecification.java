package software.wings.beans.container;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.DeploymentSpecification;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "HelmChartSpecificationKeys")
@Entity("helmChartSpecifications")
@HarnessEntity(exportable = true)
@OwnedBy(CDP)
public class HelmChartSpecification extends DeploymentSpecification implements AccountAccess {
  @NotEmpty @FdUniqueIndex private String serviceId;
  @NotNull private String chartUrl;
  @NotNull private String chartName;
  @NotNull private String chartVersion;

  public HelmChartSpecification cloneInternal() {
    HelmChartSpecification specification = HelmChartSpecification.builder()
                                               .chartName(this.chartName)
                                               .chartUrl(this.getChartUrl())
                                               .chartVersion(this.getChartVersion())
                                               .serviceId(this.serviceId)
                                               .build();
    specification.setAccountId(this.getAccountId());
    specification.setAppId(this.getAppId());
    return specification;
  }
}
