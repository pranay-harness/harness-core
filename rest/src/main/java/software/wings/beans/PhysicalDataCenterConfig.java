package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("PHYSICAL_DATA_CENTER")
public class PhysicalDataCenterConfig extends SettingValue {
  /**
   * Instantiates a new setting value.
   */
  public PhysicalDataCenterConfig() {
    super(SettingVariableTypes.PHYSICAL_DATA_CENTER.name());
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String type;

    private Builder() {}

    /**
     * A physical data center config builder.
     *
     * @return the builder
     */
    public static Builder aPhysicalDataCenterConfig() {
      return new Builder();
    }

    /**
     * With type builder.
     *
     * @param type the type
     * @return the builder
     */
    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aPhysicalDataCenterConfig().withType(type);
    }

    /**
     * Build physical data center config.
     *
     * @return the physical data center config
     */
    public PhysicalDataCenterConfig build() {
      PhysicalDataCenterConfig physicalDataCenterConfig = new PhysicalDataCenterConfig();
      physicalDataCenterConfig.setType(type);
      return physicalDataCenterConfig;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends CloudProviderYaml {
    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
    }
  }
}
