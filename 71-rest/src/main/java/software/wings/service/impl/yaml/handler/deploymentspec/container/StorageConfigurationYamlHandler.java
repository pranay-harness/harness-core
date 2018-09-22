package software.wings.service.impl.yaml.handler.deploymentspec.container;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.container.StorageConfiguration;
import software.wings.beans.container.StorageConfiguration.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
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
    return toBean(changeContext);
  }

  private StorageConfiguration toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    return StorageConfiguration.builder()
        .containerPath(yaml.getContainerPath())
        .hostSourcePath(yaml.getHostSourcePath())
        .readonly(yaml.isReadonly())
        .build();
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
