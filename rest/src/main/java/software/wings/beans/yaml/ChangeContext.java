package software.wings.beans.yaml;

import static software.wings.beans.yaml.ChangeContext.Builder.aChangeContext;

import lombok.Data;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.yaml.BaseYaml;

/**
 * @author rktummala on 10/17/17
 */
@Data
public class ChangeContext<Y extends BaseYaml> {
  private Change change;
  private YamlType yamlType;
  private Y yaml;
  private BaseYamlHandler yamlSyncHandler;
  private String appId;
  private String envId;

  public ChangeContext.Builder toBuilder() {
    return aChangeContext()
        .withChange(getChange())
        .withYamlType(getYamlType())
        .withYaml(getYaml())
        .withYamlSyncHandler(getYamlSyncHandler())
        .withAppId(getAppId())
        .withEnvId(getEnvId());
  }

  public static final class Builder {
    private Change change;
    private YamlType yamlType;
    private BaseYaml yaml;
    private BaseYamlHandler yamlSyncHandler;
    private String appId;
    private String envId;

    private Builder() {}

    public static Builder aChangeContext() {
      return new Builder();
    }

    public Builder withChange(Change change) {
      this.change = change;
      return this;
    }

    public Builder withYamlType(YamlType yamlType) {
      this.yamlType = yamlType;
      return this;
    }

    public Builder withYaml(BaseYaml yaml) {
      this.yaml = yaml;
      return this;
    }

    public Builder withYamlSyncHandler(BaseYamlHandler yamlSyncHandler) {
      this.yamlSyncHandler = yamlSyncHandler;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder but() {
      return aChangeContext()
          .withChange(change)
          .withYamlType(yamlType)
          .withYaml(yaml)
          .withYamlSyncHandler(yamlSyncHandler)
          .withAppId(appId)
          .withEnvId(envId);
    }

    public ChangeContext build() {
      ChangeContext changeContext = new ChangeContext();
      changeContext.setChange(change);
      changeContext.setYamlType(yamlType);
      changeContext.setYaml(yaml);
      changeContext.setYamlSyncHandler(yamlSyncHandler);
      changeContext.setAppId(appId);
      changeContext.setEnvId(envId);
      return changeContext;
    }
  }
}
