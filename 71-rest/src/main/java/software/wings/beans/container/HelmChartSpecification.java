package software.wings.beans.container;

import io.harness.annotation.HarnessEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.DeploymentSpecification;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "HelmChartSpecificationKeys")
@Entity("helmChartSpecifications")
@HarnessEntity(exportable = true)
public class HelmChartSpecification extends DeploymentSpecification {
  @NotEmpty @Indexed(options = @IndexOptions(unique = true)) private String serviceId;
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

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends DeploymentSpecification.Yaml {
    private String chartUrl;
    private String chartName;
    private String chartVersion;

    @Builder
    public Yaml(String type, String harnessApiVersion, String chartUrl, String chartName, String chartVersion) {
      super(type, harnessApiVersion);
      this.chartUrl = chartUrl;
      this.chartName = chartName;
      this.chartVersion = chartVersion;
    }
  }
}
