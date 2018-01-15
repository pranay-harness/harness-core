package software.wings.service.impl.yaml.handler.deploymentspec.container;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;

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
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Util;
import software.wings.utils.Validator;

import java.util.List;
/**
 * @author rktummala on 11/15/17
 */
public abstract class ContainerTaskYamlHandler<Y extends ContainerTask.Yaml, C extends ContainerTask>
    extends DeploymentSpecificationYamlHandler<Y, C> {
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;

  protected C toBean(C containerTask, ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Y yaml = changeContext.getYaml();
    Change change = changeContext.getChange();

    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
    Validator.notNullCheck("Could not locate app info in file path:" + change.getFilePath(), appId);

    String serviceId = yamlHelper.getServiceId(appId, change.getFilePath());
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
        ContainerDefinition containerDefinition =
            (ContainerDefinition) containerDefYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
        containerTask.setContainerDefinitions(asList(containerDefinition));
      } catch (HarnessException e) {
        throw new WingsException(e);
      }
    }

    return containerTask;
  }

  protected void toYaml(Y yaml, C bean) {
    String advancedType = Util.getStringFromEnum(bean.getAdvancedType());

    yaml.setHarnessApiVersion(getHarnessApiVersion());
    yaml.setAdvancedConfig(bean.getAdvancedConfig());
    yaml.setAdvancedType(advancedType);
    yaml.setType(bean.getDeploymentType());

    // container definition
    BaseYamlHandler containerDefYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.CONTAINER_DEFINITION, ObjectType.CONTAINER_DEFINITION);
    List<ContainerDefinition> containerDefinitions = bean.getContainerDefinitions();
    if (isNotEmpty(containerDefinitions)) {
      ContainerDefinition containerDefinition = containerDefinitions.get(0);
      ContainerDefinition.Yaml containerDefYaml =
          (ContainerDefinition.Yaml) containerDefYamlHandler.toYaml(containerDefinition, bean.getAppId());
      yaml.setContainerDefinition(containerDefYaml);
    }
  }

  @Override
  public boolean validate(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) {
    Y yaml = changeContext.getYaml();
    return !(yaml == null || yaml.getType() == null);
  }

  protected C getContainerTask(String accountId, String yamlFilePath, String deploymentType) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Could not locate app info in file path:" + yamlFilePath, appId);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Could not locate service info in file path:" + yamlFilePath, serviceId);

    return (C) serviceResourceService.getContainerTaskByDeploymentType(appId, serviceId, deploymentType);
  }

  @Override
  public C upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    C previous = get(accountId, changeContext.getChange().getFilePath());
    C containerTask = toBean(previous, changeContext, changeSetContext);
    if (previous != null) {
      return (C) serviceResourceService.updateContainerTask(containerTask, false);
    } else {
      return (C) serviceResourceService.createContainerTask(containerTask, false);
    }
  }
}
