package software.wings.yaml;

import lombok.Data;

/**
 * @author rktummala on 10/03/17
 */
@Data
public class EntityVersionYaml extends BaseYaml {
  @YamlSerialize private int version;

  public static final class Builder {
    private int version;

    private Builder() {}

    public static Builder anEntityVersionYaml() {
      return new Builder();
    }

    public Builder withVersion(int version) {
      this.version = version;
      return this;
    }

    public Builder but() {
      return anEntityVersionYaml().withVersion(version);
    }

    public EntityVersionYaml build() {
      EntityVersionYaml entityVersionYaml = new EntityVersionYaml();
      entityVersionYaml.setVersion(version);
      return entityVersionYaml;
    }
  }
}
