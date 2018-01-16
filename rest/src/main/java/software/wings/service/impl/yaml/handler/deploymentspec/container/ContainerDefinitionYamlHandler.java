package software.wings.service.impl.yaml.handler.deploymentspec.container;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.ErrorCode;
import software.wings.beans.ObjectType;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ContainerDefinition.Yaml;
import software.wings.beans.container.LogConfiguration;
import software.wings.beans.container.PortMapping;
import software.wings.beans.container.StorageConfiguration;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
/**
 * @author rktummala on 11/15/17
 */
@Singleton
public class ContainerDefinitionYamlHandler extends BaseYamlHandler<ContainerDefinition.Yaml, ContainerDefinition> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;

  @Override
  public ContainerDefinition.Yaml toYaml(ContainerDefinition containerDefinition, String appId) {
    // Log Configuration
    BaseYamlHandler logConfigYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.LOG_CONFIGURATION, ObjectType.LOG_CONFIGURATION);
    LogConfiguration.Yaml logConfigYaml = null;
    if (containerDefinition.getLogConfiguration() != null) {
      logConfigYaml =
          (LogConfiguration.Yaml) logConfigYamlHandler.toYaml(containerDefinition.getLogConfiguration(), appId);
    }

    // Port Mappings
    List<PortMapping.Yaml> portMappingYamlList = Collections.emptyList();
    BaseYamlHandler portMappingYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.PORT_MAPPING, ObjectType.PORT_MAPPING);
    List<PortMapping> portMappings = containerDefinition.getPortMappings();
    if (isNotEmpty(portMappings)) {
      portMappingYamlList =
          portMappings.stream()
              .map(portMapping -> (PortMapping.Yaml) portMappingYamlHandler.toYaml(portMapping, appId))
              .collect(Collectors.toList());
    }

    // Storage Configurations
    List<StorageConfiguration.Yaml> storageConfigYamlList = Collections.emptyList();
    BaseYamlHandler storageConfigYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.STORAGE_CONFIGURATION, ObjectType.STORAGE_CONFIGURATION);
    List<StorageConfiguration> storageConfigurations = containerDefinition.getStorageConfigurations();
    if (isNotEmpty(storageConfigurations)) {
      storageConfigYamlList =
          storageConfigurations.stream()
              .filter(storageConfiguration
                  -> isNotBlank(storageConfiguration.getHostSourcePath())
                      && isNotBlank(storageConfiguration.getContainerPath()))
              .map(storageConfiguration
                  -> (StorageConfiguration.Yaml) storageConfigYamlHandler.toYaml(storageConfiguration, appId))
              .collect(Collectors.toList());
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
      BaseYamlHandler portMappingYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.PORT_MAPPING, ObjectType.PORT_MAPPING);
      portMappings =
          yaml.getPortMappings()
              .stream()
              .map(portMapping -> {
                try {
                  ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, portMapping);
                  return (PortMapping) portMappingYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
                } catch (HarnessException e) {
                  throw new WingsException(e);
                }
              })
              .collect(Collectors.toList());
    }

    // storage configurations
    List<StorageConfiguration> storageConfigs = Lists.newArrayList();
    if (isNotEmpty(yaml.getStorageConfigurations())) {
      BaseYamlHandler storageConfigYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.STORAGE_CONFIGURATION, ObjectType.STORAGE_CONFIGURATION);
      storageConfigs = yaml.getStorageConfigurations()
                           .stream()
                           .map(storageConfig -> {
                             try {
                               ChangeContext.Builder clonedContext =
                                   cloneFileChangeContext(changeContext, storageConfig);
                               return (StorageConfiguration) storageConfigYamlHandler.upsertFromYaml(
                                   clonedContext.build(), changeSetContext);
                             } catch (HarnessException e) {
                               throw new WingsException(e);
                             }
                           })
                           .collect(Collectors.toList());
    }

    // log configuration
    LogConfiguration logConfig = null;
    LogConfiguration.Yaml logConfigYaml = yaml.getLogConfiguration();
    if (logConfigYaml != null) {
      BaseYamlHandler logConfigYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.LOG_CONFIGURATION, ObjectType.LOG_CONFIGURATION);
      ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, logConfigYaml);
      logConfig = (LogConfiguration) logConfigYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
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
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml applicationYaml = changeContext.getYaml();
    return !(isEmpty(applicationYaml.getName()));
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
