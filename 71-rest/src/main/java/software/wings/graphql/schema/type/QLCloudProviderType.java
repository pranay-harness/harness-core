package software.wings.graphql.schema.type;

/**
 * @author rktummala on 07/18/19
 */
public enum QLCloudProviderType implements QLEnum {
  PHYSICAL_DATA_CENTER,
  AWS,
  AZURE,
  GCP,
  KUBERNETES_CLUSTER,
  PCF;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
