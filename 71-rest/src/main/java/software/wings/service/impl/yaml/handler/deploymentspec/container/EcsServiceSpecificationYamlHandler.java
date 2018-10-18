package software.wings.service.impl.yaml.handler.deploymentspec.container;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Service;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.EcsServiceSpecification.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.common.Constants;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Validator;

import java.util.List;

@Singleton
public class EcsServiceSpecificationYamlHandler
    extends DeploymentSpecificationYamlHandler<Yaml, EcsServiceSpecification> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public Yaml toYaml(EcsServiceSpecification bean, String appId) {
    Service service = serviceResourceService.get(appId, bean.getServiceId());
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(Constants.ECS_SERVICE_SPEC)
        .serviceName(service.getName())
        .serviceSpecJson(bean.getServiceSpecJson())
        .schedulingStrategy(bean.getSchedulingStrategy())
        .build();
  }

  @Override
  public EcsServiceSpecification upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    EcsServiceSpecification previous =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());

    EcsServiceSpecification ecsServiceSpecification = toBean(changeContext);
    ecsServiceSpecification.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      ecsServiceSpecification.setUuid(previous.getUuid());
      return serviceResourceService.updateEcsServiceSpecification(ecsServiceSpecification);
    } else {
      return serviceResourceService.createEcsServiceSpecification(ecsServiceSpecification);
    }
  }

  private EcsServiceSpecification toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    String filePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), filePath);
    Validator.notNullCheck("Could not lookup app for the yaml file: " + filePath, appId);

    String serviceId = yamlHelper.getServiceId(appId, filePath);
    Validator.notNullCheck("Could not lookup service for the yaml file: " + filePath, serviceId);

    EcsServiceSpecification pcfServiceSpecification = EcsServiceSpecification.builder()
                                                          .schedulingStrategy(yaml.getSchedulingStrategy())
                                                          .serviceSpecJson(yaml.getServiceSpecJson())
                                                          .serviceId(serviceId)
                                                          .build();
    pcfServiceSpecification.setAppId(appId);
    return pcfServiceSpecification;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public EcsServiceSpecification get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Could not lookup service for the yaml file: " + yamlFilePath, serviceId);

    return serviceResourceService.getEcsServiceSpecification(appId, serviceId);
  }
}
