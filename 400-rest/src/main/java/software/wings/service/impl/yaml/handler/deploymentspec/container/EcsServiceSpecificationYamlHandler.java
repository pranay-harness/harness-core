package software.wings.service.impl.yaml.handler.deploymentspec.container;

import static io.harness.validation.Validator.notNullCheck;

import software.wings.beans.Service;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.EcsServiceSpecificationYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class EcsServiceSpecificationYamlHandler
    extends DeploymentSpecificationYamlHandler<EcsServiceSpecificationYaml, EcsServiceSpecification> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public EcsServiceSpecificationYaml toYaml(EcsServiceSpecification bean, String appId) {
    Service service = serviceResourceService.getWithDetails(appId, bean.getServiceId());
    return EcsServiceSpecificationYaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(YamlHandlerFactory.ECS_SERVICE_SPEC)
        .serviceName(service.getName())
        .serviceSpecJson(bean.getServiceSpecJson())
        .build();
  }

  @Override
  public EcsServiceSpecification upsertFromYaml(
      ChangeContext<EcsServiceSpecificationYaml> changeContext, List<ChangeContext> changeSetContext) {
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

  private EcsServiceSpecification toBean(ChangeContext<EcsServiceSpecificationYaml> changeContext) {
    EcsServiceSpecificationYaml yaml = changeContext.getYaml();

    String filePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), filePath);
    notNullCheck("Could not lookup app for the yaml file: " + filePath, appId);

    String serviceId = yamlHelper.getServiceId(appId, filePath);
    notNullCheck("Could not lookup service for the yaml file: " + filePath, serviceId);

    EcsServiceSpecification ecsServiceSpecification =
        EcsServiceSpecification.builder().serviceSpecJson(yaml.getServiceSpecJson()).serviceId(serviceId).build();
    ecsServiceSpecification.setAppId(appId);
    return ecsServiceSpecification;
  }

  @Override
  public Class getYamlClass() {
    return EcsServiceSpecificationYaml.class;
  }

  @Override
  public EcsServiceSpecification get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Could not lookup service for the yaml file: " + yamlFilePath, serviceId);

    return serviceResourceService.getEcsServiceSpecification(appId, serviceId);
  }

  @Override
  public void delete(ChangeContext<EcsServiceSpecificationYaml> changeContext) {
    EcsServiceSpecification ecsServiceSpecification =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (ecsServiceSpecification != null) {
      serviceResourceService.resetToDefaultEcsServiceSpecification(
          ecsServiceSpecification.getAppId(), ecsServiceSpecification.getServiceId());
    }
  }
}
