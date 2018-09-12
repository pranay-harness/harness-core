package software.wings.service.impl.yaml.handler.deploymentspec.container;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ContainerDefinition.Yaml;
import software.wings.beans.container.LogConfiguration;
import software.wings.beans.container.PortMapping;
import software.wings.beans.container.StorageConfiguration;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;

import java.util.Collections;
import java.util.List;
/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class ContainerDefinitionYamlHandler extends BaseYamlHandler<ContainerDefinition.Yaml, ContainerDefinition> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;

  @Override
  public ContainerDefinition.Yaml toYaml(ContainerDefinition containerDefinition, String appId) {
    // Log Configuration
    LogConfigurationYamlHandler logConfigYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.LOG_CONFIGURATION);
    LogConfiguration.Yaml logConfigYaml = null;
    if (containerDefinition.getLogConfiguration() != null) {
      logConfigYaml = logConfigYamlHandler.toYaml(containerDefinition.getLogConfiguration(), appId);
    }

    // Port Mappings
    List<PortMapping.Yaml> portMappingYamlList = Collections.emptyList();
    PortMappingYamlHandler portMappingYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.PORT_MAPPING);
    List<PortMapping> portMappings = containerDefinition.getPortMappings();
    if (isNotEmpty(portMappings)) {
      portMappingYamlList =
          portMappings.stream().map(portMapping -> portMappingYamlHandler.toYaml(portMapping, appId)).collect(toList());
    }

    // Storage Configurations
    List<StorageConfiguration.Yaml> storageConfigYamlList = Collections.emptyList();
    StorageConfigurationYamlHandler storageConfigYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.STORAGE_CONFIGURATION);
    List<StorageConfiguration> storageConfigurations = containerDefinition.getStorageConfigurations();
    if (isNotEmpty(storageConfigurations)) {
      storageConfigYamlList =
          storageConfigurations.stream()
              .filter(storageConfiguration
                  -> isNotBlank(storageConfiguration.getHostSourcePath())
                      && isNotBlank(storageConfiguration.getContainerPath()))
              .map(storageConfiguration -> storageConfigYamlHandler.toYaml(storageConfiguration, appId))
              .collect(toList());
    }

    return Yaml.builder()
        .commands(containerDefinition.getCommands())
        .cpu(containerDefinition.getCpu())
        .logConfiguration(logConfigYaml)
        .memory(containerDefinition.getMemory())
        .name(containerDefinition.getName())
        .portMappings(portMappingYamlList)
        .storageConfigurations(storageConfigYamlList)
        .build();
  }

  @Override
  public ContainerDefinition upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext, changeSetContext);
  }

  private ContainerDefinition toBean(ChangeContext<ContainerDefinition.Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    // port mappings
    List<PortMapping> portMappings = Lists.newArrayList();
    if (yaml.getPortMappings() != null) {
      PortMappingYamlHandler portMappingYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.PORT_MAPPING);
      portMappings = yaml.getPortMappings()
                         .stream()
                         .map(portMapping -> {
                           try {
                             ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, portMapping);
                             return portMappingYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
                           } catch (HarnessException e) {
                             throw new WingsException(e);
                           }
                         })
                         .collect(toList());
    }

    // storage configurations
    List<StorageConfiguration> storageConfigs = Lists.newArrayList();
    if (isNotEmpty(yaml.getStorageConfigurations())) {
      StorageConfigurationYamlHandler storageConfigYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.STORAGE_CONFIGURATION);
      storageConfigs = yaml.getStorageConfigurations()
                           .stream()
                           .map(storageConfig -> {
                             try {
                               ChangeContext.Builder clonedContext =
                                   cloneFileChangeContext(changeContext, storageConfig);
                               return storageConfigYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
                             } catch (HarnessException e) {
                               throw new WingsException(e);
                             }
                           })
                           .collect(toList());
    }

    // log configuration
    LogConfiguration logConfig = null;
    LogConfiguration.Yaml logConfigYaml = yaml.getLogConfiguration();
    if (logConfigYaml != null) {
      LogConfigurationYamlHandler logConfigYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.LOG_CONFIGURATION);
      ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, logConfigYaml);
      logConfig = logConfigYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
    }

    return ContainerDefinition.builder()
        .commands(yaml.getCommands())
        .cpu(yaml.getCpu())
        .logConfiguration(logConfig)
        .memory(yaml.getMemory())
        .name(yaml.getName())
        .portMappings(portMappings)
        .storageConfigurations(storageConfigs)
        .build();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public ContainerDefinition get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
