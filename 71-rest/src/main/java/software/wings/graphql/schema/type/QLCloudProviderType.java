package software.wings.graphql.schema.type;

/**
 * @author rktummala on 07/18/19
 */
public enum QLCloudProviderType implements QLEnum {
  AWS,
  PHYSICAL_DATA_CENTER,
  AZURE,
  GCP,
  KUBERNETES,
  PCF;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
