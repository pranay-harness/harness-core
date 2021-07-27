package software.wings.graphql.schema.type;

/**
 * @author rktummala on 07/18/19
 */
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public enum QLCloudProviderType implements QLEnum {
  PHYSICAL_DATA_CENTER,
  AWS,
  AZURE,
  GCP,
  KUBERNETES_CLUSTER,
  PCF,
  SPOT_INST;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
