package software.wings.service.impl.yaml.handler.deploymentspec.container;

import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;

import software.wings.beans.container.StorageConfiguration;
import software.wings.beans.container.StorageConfigurationYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class StorageConfigurationYamlHandler extends BaseYamlHandler<StorageConfigurationYaml, StorageConfiguration> {
  @Override
  public StorageConfigurationYaml toYaml(StorageConfiguration storageConfiguration, String appId) {
    return StorageConfigurationYaml.builder()
        .containerPath(storageConfiguration.getContainerPath())
        .hostSourcePath(storageConfiguration.getHostSourcePath())
        .readonly(storageConfiguration.isReadonly())
        .build();
  }

  @Override
  public StorageConfiguration upsertFromYaml(ChangeContext<StorageConfigurationYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    return toBean(changeContext);
  }

  private StorageConfiguration toBean(ChangeContext<StorageConfigurationYaml> changeContext) {
    StorageConfigurationYaml yaml = changeContext.getYaml();
    return StorageConfiguration.builder()
        .containerPath(yaml.getContainerPath())
        .hostSourcePath(yaml.getHostSourcePath())
        .readonly(yaml.isReadonly())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return StorageConfigurationYaml.class;
  }

  @Override
  public StorageConfiguration get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<StorageConfigurationYaml> changeContext) {
    // Do nothing
  }
}
