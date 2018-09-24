package software.wings.service.impl.yaml.handler.deploymentspec.container;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.api.DeploymentType;
import software.wings.beans.Service;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.PcfServiceSpecification.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Validator;

import java.util.List;

@Singleton
public class PcfServiceSpecificationYamlHandler
    extends DeploymentSpecificationYamlHandler<Yaml, PcfServiceSpecification> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public Yaml toYaml(PcfServiceSpecification bean, String appId) {
    Service service = serviceResourceService.get(appId, bean.getServiceId());
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(DeploymentType.PCF.name())
        .serviceName(service.getName())
        .manifestYaml(bean.getManifestYaml())
        .build();
  }

  @Override
  public PcfServiceSpecification upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    PcfServiceSpecification previous =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());

    PcfServiceSpecification pcfServiceSpecification = toBean(changeContext);
    pcfServiceSpecification.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      pcfServiceSpecification.setUuid(previous.getUuid());
      return serviceResourceService.updatePcfServiceSpecification(pcfServiceSpecification);
    } else {
      return serviceResourceService.createPcfServiceSpecification(pcfServiceSpecification);
    }
  }

  private PcfServiceSpecification toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    String filePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), filePath);
    Validator.notNullCheck("Could not lookup app for the yaml file: " + filePath, appId);

    String serviceId = yamlHelper.getServiceId(appId, filePath);
    Validator.notNullCheck("Could not lookup service for the yaml file: " + filePath, serviceId);

    PcfServiceSpecification pcfServiceSpecification =
        PcfServiceSpecification.builder().manifestYaml(yaml.getManiefstYaml()).serviceId(serviceId).build();
    pcfServiceSpecification.setAppId(appId);
    return pcfServiceSpecification;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public PcfServiceSpecification get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Could not lookup service for the yaml file: " + yamlFilePath, serviceId);

    return serviceResourceService.getPcfServiceSpecification(appId, serviceId);
  }
}
