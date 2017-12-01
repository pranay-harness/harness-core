package software.wings.service.impl.yaml.handler.deploymentspec;

import com.google.inject.Inject;

import software.wings.beans.ObjectType;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ContainerTask.AdvancedType;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Util;
import software.wings.utils.Validator;

import java.util.Arrays;
import java.util.List;

/**
 * @author rktummala on 11/15/17
 */
public abstract class ContainerTaskYamlHandler<Y extends ContainerTask.Yaml, C extends ContainerTask>
    extends DeploymentSpecificationYamlHandler<Y, C> {
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject YamlSyncHelper yamlSyncHelper;
  @Inject ServiceResourceService serviceResourceService;

  protected abstract Y getContainerTaskYaml();

  @Override
  public C createFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    C containerTask = setWithYamlValues(null, changeContext, changeSetContext);
    return (C) serviceResourceService.createContainerTask(containerTask, false);
  }

  protected C setWithYamlValues(C containerTask, ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Y yaml = changeContext.getYaml();
    Change change = changeContext.getChange();

    String appId = yamlSyncHelper.getAppId(change.getAccountId(), change.getFilePath());
    Validator.notNullCheck("Could not locate app info in file path:" + change.getFilePath(), appId);

    String serviceId = yamlSyncHelper.getServiceId(appId, change.getFilePath());
    Validator.notNullCheck("Could not locate service info in file path:" + change.getFilePath(), serviceId);

    AdvancedType advancedType = Util.getEnumFromString(AdvancedType.class, yaml.getAdvancedType());
    containerTask.setServiceId(serviceId);
    containerTask.setAppId(appId);
    containerTask.setAdvancedConfig(yaml.getAdvancedConfig());
    containerTask.setAdvancedType(advancedType);

    // container definition
    if (yaml.getContainerDefinition() != null) {
      BaseYamlHandler containerDefYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.CONTAINER_DEFINITION, ObjectType.CONTAINER_DEFINITION);
      try {
        ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, yaml.getContainerDefinition());
        ContainerDefinition containerDefinition = (ContainerDefinition) createOrUpdateFromYaml(
            containerTask == null, containerDefYamlHandler, clonedContext.build(), changeSetContext);
        containerTask.setContainerDefinitions(Arrays.asList(containerDefinition));
      } catch (HarnessException e) {
        throw new WingsException(e);
      }
    }

    return containerTask;
  }

  @Override
  public Y toYaml(C bean, String appId) {
    String advancedType = Util.getStringFromEnum(bean.getAdvancedType());

    Y yaml = getContainerTaskYaml();

    yaml.setAdvancedConfig(bean.getAdvancedConfig());
    yaml.setAdvancedType(advancedType);
    yaml.setType(bean.getDeploymentType());

    // container definition
    BaseYamlHandler containerDefYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.CONTAINER_DEFINITION, ObjectType.CONTAINER_DEFINITION);
    List<ContainerDefinition> containerDefinitions = bean.getContainerDefinitions();
    if (!Util.isEmpty(containerDefinitions)) {
      ContainerDefinition containerDefinition = containerDefinitions.get(0);
      ContainerDefinition.Yaml containerDefYaml =
          (ContainerDefinition.Yaml) containerDefYamlHandler.toYaml(containerDefinition, appId);
      yaml.setContainerDefinition(containerDefYaml);
    }

    return yaml;
  }

  @Override
  public boolean validate(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) {
    Y yaml = changeContext.getYaml();
    return !(yaml == null || yaml.getType() == null);
  }

  protected C getContainerTask(String accountId, String yamlFilePath, String deploymentType) {
    String appId = yamlSyncHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Could not locate app info in file path:" + yamlFilePath, appId);

    String serviceId = yamlSyncHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Could not locate service info in file path:" + yamlFilePath, serviceId);

    return (C) serviceResourceService.getContainerTaskByDeploymentType(appId, serviceId, deploymentType);
  }

  @Override
  public C updateFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    C previous = get(accountId, changeContext.getChange().getFilePath());
    C containerTask = setWithYamlValues(previous, changeContext, changeSetContext);
    return (C) serviceResourceService.updateContainerTask(containerTask, false);
  }

  @Override
  public C upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    C previous = get(accountId, changeContext.getChange().getFilePath());
    C containerTask = setWithYamlValues(previous, changeContext, changeSetContext);
    if (previous != null) {
      return (C) serviceResourceService.updateContainerTask(containerTask, false);
    } else {
      return (C) serviceResourceService.createContainerTask(containerTask, false);
    }
  }

  //  @Override
  //  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
  //    C containerTask = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
  //    if (containerTask != null) {
  //      serviceResourceService.deleteContainerTask(containerTask.getAppId(), containerTask.getUuid());
  //    }
  //  }
}
