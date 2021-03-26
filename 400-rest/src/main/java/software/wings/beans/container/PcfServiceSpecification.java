package software.wings.beans.container;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.yaml.YamlHelper.trimYaml;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.AccountAccess;

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
@FieldNameConstants(innerTypeName = "PcfServiceSpecificationKeys")
@Entity("pcfServiceSpecification")
@HarnessEntity(exportable = true)
@OwnedBy(CDP)
public class PcfServiceSpecification extends DeploymentSpecification implements AccountAccess {
  @NotNull private String serviceId;
  @NotNull private String manifestYaml;

  public static final String preamble = "# Enter your Task Definition JSON spec below.\n"
      + "#\n"
      + "# Placeholders:\n"
      + "#\n"
      + "# Required: {APPLICATION_NAME}\n"
      + "#   - Replaced with the application name being deployed\n"
      + "#\n"
      + "# Optional: {INSTANCE_COUNT}\n"
      + "#   - Replaced with a instance count for application\n"
      + "#\n"
      + "# Required: {FILE_LOCATION}\n"
      + "#   - Replaced with file location\n"
      + "#\n"
      + "# Required: {ROUTE_MAP}\n"
      + "#   - Replaced with route maps\n"
      + "#\n"
      + "# ---\n\n";

  public static final String manifestTemplate = "applications:\n"
      + "- name: ${APPLICATION_NAME}\n"
      + "  memory: 750M\n"
      + "  INSTANCES : ${INSTANCE_COUNT}\n"
      + "  path: ${FILE_LOCATION}\n"
      + "  ROUTES:\n"
      + "  - route: ${ROUTE_MAP}";

  public void resetToDefaultManifestSpecification() {
    this.manifestYaml = trimYaml(preamble + manifestTemplate);
  }

  public PcfServiceSpecification cloneInternal() {
    PcfServiceSpecification specification =
        PcfServiceSpecification.builder().serviceId(this.serviceId).manifestYaml(this.getManifestYaml()).build();
    specification.setAppId(this.getAppId());
    specification.setAccountId(this.getAccountId());
    return specification;
  }
}
