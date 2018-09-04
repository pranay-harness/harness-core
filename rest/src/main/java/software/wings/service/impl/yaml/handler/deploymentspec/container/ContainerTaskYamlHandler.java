package software.wings.service.impl.yaml.handler.deploymentspec.container;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;
/**
 * @author rktummala on 11/15/17
 */
public abstract class ContainerTaskYamlHandler<Y extends ContainerTask.Yaml, C extends ContainerTask>
    extends DeploymentSpecificationYamlHandler<Y, C> {
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;

  protected abstract C createNewContainerTask();

  protected C toBean(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    Y yaml = changeContext.getYaml();
    Change change = changeContext.getChange();

    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
    notNullCheck("Could not locate app info in file path:" + change.getFilePath(), appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, change.getFilePath());
    notNullCheck("Could not locate service info in file path:" + change.getFilePath(), serviceId, USER);

    C containerTask = createNewContainerTask();

    containerTask.setServiceId(serviceId);
    containerTask.setAppId(appId);
    containerTask.setAdvancedConfig(yaml.getAdvancedConfig());

    // container definition
    if (yaml.getAdvancedConfig() == null && yaml.getContainerDefinition() != null) {
      ContainerDefinitionYamlHandler containerDefYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.CONTAINER_DEFINITION);
      try {
        ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, yaml.getContainerDefinition());
        ContainerDefinition containerDefinition =
            containerDefYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
        containerTask.setContainerDefinitions(asList(containerDefinition));
      } catch (HarnessException e) {
        throw new WingsException(e);
      }
    } else {
      ContainerDefinition containerDefinition = ContainerDefinition.builder().name("DEFAULT_NAME").build();
      containerTask.setContainerDefinitions(asList(containerDefinition));
    }

    return containerTask;
  }

  protected void toYaml(Y yaml, C bean) {
    yaml.setHarnessApiVersion(getHarnessApiVersion());
    yaml.setAdvancedConfig(bean.getAdvancedConfig());
    yaml.setType(bean.getDeploymentType());

    if (bean.getAdvancedConfig() == null) {
      // container definition
      ContainerDefinitionYamlHandler containerDefYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.CONTAINER_DEFINITION);
      List<ContainerDefinition> containerDefinitions = bean.getContainerDefinitions();
      if (isNotEmpty(containerDefinitions)) {
        ContainerDefinition containerDefinition = containerDefinitions.get(0);
        ContainerDefinition.Yaml containerDefYaml =
            containerDefYamlHandler.toYaml(containerDefinition, bean.getAppId());
        yaml.setContainerDefinition(containerDefYaml);
      }
    }
  }

  protected C getContainerTask(String accountId, String yamlFilePath, String deploymentType) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not locate app info in file path:" + yamlFilePath, appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Could not locate service info in file path:" + yamlFilePath, serviceId, USER);

    return (C) serviceResourceService.getContainerTaskByDeploymentType(appId, serviceId, deploymentType);
  }

  @Override
  public C upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    C previous = get(accountId, changeContext.getChange().getFilePath());
    C containerTask = toBean(changeContext, changeSetContext);
    containerTask.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      containerTask.setUuid(previous.getUuid());
      return (C) serviceResourceService.updateContainerTask(containerTask, false);
    } else {
      return (C) serviceResourceService.createContainerTask(containerTask, false);
    }
  }
}
