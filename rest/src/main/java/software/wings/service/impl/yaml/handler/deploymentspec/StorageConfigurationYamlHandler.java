package software.wings.service.impl.yaml.handler.deploymentspec;

import software.wings.beans.ErrorCode;
import software.wings.beans.container.StorageConfiguration;
import software.wings.beans.container.StorageConfiguration.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
public class StorageConfigurationYamlHandler extends BaseYamlHandler<Yaml, StorageConfiguration> {
  @Override
  public Yaml toYaml(StorageConfiguration storageConfiguration, String appId) {
    return Yaml.builder()
        .containerPath(storageConfiguration.getContainerPath())
        .hostSourcePath(storageConfiguration.getHostSourcePath())
        .readonly(storageConfiguration.isReadonly())
        .build();
  }

  @Override
  public StorageConfiguration upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public StorageConfiguration updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  private StorageConfiguration setWithYamlValues(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    return StorageConfiguration.builder()
        .containerPath(yaml.getContainerPath())
        .hostSourcePath(yaml.getHostSourcePath())
        .readonly(yaml.isReadonly())
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public StorageConfiguration createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public StorageConfiguration get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
