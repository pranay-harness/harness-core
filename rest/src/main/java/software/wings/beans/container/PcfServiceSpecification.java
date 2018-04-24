package software.wings.beans.container;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.DeploymentSpecification;

import javax.validation.constraints.NotNull;

@Entity("pcfServiceSpecification")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class PcfServiceSpecification extends DeploymentSpecification {
  @NotNull private String serviceId;
  @NotNull private String maniefstYaml;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends DeploymentSpecification.Yaml {
    private String maniefstYaml;
    private String serviceName;

    @Builder
    public Yaml(String type, String harnessApiVersion, String serviceName, String manifestYaml) {
      super(type, harnessApiVersion);
      this.maniefstYaml = manifestYaml;
      this.serviceName = serviceName;
    }
  }
}
