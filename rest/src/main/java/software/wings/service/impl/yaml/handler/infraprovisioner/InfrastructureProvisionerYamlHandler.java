package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisioner.Yaml;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.NameValuePairYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class InfrastructureProvisionerYamlHandler<Y extends Yaml, B extends InfrastructureProvisioner>
    extends BaseYamlHandler<Y, B> {
  @Inject YamlHelper yamlHelper;
  @Inject InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject YamlHandlerFactory yamlHandlerFactory;

  private InfrastructureMappingBlueprint.Yaml mapBluePrintBeanToYaml(
      InfrastructureMappingBlueprint blueprint, String appId) {
    NameValuePairYamlHandler nameValuePairYamlHandler = getNameValuePairYamlHandler();

    List<NameValuePair.Yaml> nameValueYamls = Collections.EMPTY_LIST;
    if (EmptyPredicate.isNotEmpty(blueprint.getProperties())) {
      nameValueYamls = blueprint.getProperties()
                           .stream()
                           .map(nameValuePair -> nameValuePairYamlHandler.toYaml(nameValuePair, appId))
                           .collect(toList());
    }

    return InfrastructureMappingBlueprint.Yaml.builder()
        .cloudProviderType(blueprint.getCloudProviderType())
        .deploymentType(blueprint.getDeploymentType())
        .nodeFilteringType(blueprint.getNodeFilteringType())
        .serviceName(getServiceName(appId, blueprint.getServiceId()))
        .properties(Util.getSortedNameValuePairYamlList(nameValueYamls))
        .build();
  }

  private NameValuePairYamlHandler getNameValuePairYamlHandler() {
    return yamlHandlerFactory.getYamlHandler(YamlType.NAME_VALUE_PAIR);
  }

  public Yaml toYaml(InfrastructureProvisioner.Yaml yaml, InfrastructureProvisioner bean) {
    yaml.setName(bean.getName());
    yaml.setDescription(bean.getDescription());
    yaml.setHarnessApiVersion(getHarnessApiVersion());

    if (isNotEmpty(bean.getMappingBlueprints())) {
      yaml.setMappingBlueprints(bean.getMappingBlueprints()
                                    .stream()
                                    .map(blueprint -> mapBluePrintBeanToYaml(blueprint, bean.getAppId()))
                                    .collect(toList()));
    }

    if (isNotEmpty(bean.getVariables())) {
      NameValuePairYamlHandler nameValuePairYamlHandler = getNameValuePairYamlHandler();
      List<NameValuePair.Yaml> nvpYamlList =
          bean.getVariables()
              .stream()
              .map(nameValuePair -> nameValuePairYamlHandler.toYaml(nameValuePair, bean.getAppId()))
              .collect(toList());

      yaml.setVariables(Util.getSortedNameValuePairYamlList(nvpYamlList));
    }

    return yaml;
  }

  protected String getServiceId(String appId, String serviceName) {
    Service service = serviceResourceService.getServiceByName(appId, serviceName);
    notNullCheck("Invalid Service:" + serviceName, service, USER);
    return service.getUuid();
  }

  protected String getServiceName(String appId, String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    notNullCheck("Invalid Service:" + serviceId, service, USER);
    return service.getName();
  }

  protected InfrastructureMappingBlueprint mapBluePrintYamlToBean(
      InfrastructureMappingBlueprint.Yaml yaml, String appId, String yamlFilePath) {
    String serviceId = getServiceId(appId, yaml.getServiceName());
    notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId, USER);

    List<NameValuePair> nameValuePairList = null;
    if (yaml.getProperties() != null) {
      nameValuePairList =
          yaml.getProperties()
              .stream()
              .map(nvpYaml -> NameValuePair.builder().name(nvpYaml.getName()).value(nvpYaml.getValue()).build())
              .collect(toList());
    }

    return InfrastructureMappingBlueprint.builder()
        .cloudProviderType(yaml.getCloudProviderType())
        .deploymentType(yaml.getDeploymentType())
        .nodeFilteringType(yaml.getNodeFilteringType())
        .serviceId(serviceId)
        .properties(nameValuePairList)
        .build();
  }

  @Override
  public Class getYamlClass() {
    return InfrastructureProvisioner.Yaml.class;
  }

  public void toBean(ChangeContext<Y> changeContext, B bean, String appId, String yamlPath) {
    Y yaml = changeContext.getYaml();

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    bean.setAppId(appId);
    bean.setName(name);
    bean.setDescription(yaml.getDescription());

    if (isNotEmpty(yaml.getMappingBlueprints())) {
      List<InfrastructureMappingBlueprint> infrastructureMappingBlueprints =
          yaml.getMappingBlueprints()
              .stream()
              .map(bluePrint -> mapBluePrintYamlToBean(bluePrint, appId, yamlPath))
              .collect(toList());
      bean.setMappingBlueprints(infrastructureMappingBlueprints);
    }

    if (isNotEmpty(yaml.getVariables())) {
      List<NameValuePair> nameValuePairList =
          yaml.getVariables()
              .stream()
              .map(nvpYaml -> NameValuePair.builder().name(nvpYaml.getName()).value(nvpYaml.getValue()).build())
              .collect(toList());

      bean.setVariables(nameValuePairList);
    }
  }

  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    InfrastructureProvisioner infrastructureProvisioner =
        yamlHelper.getInfrastructureProvisionerByAppIdYamlPath(optionalApplication.get().getUuid(), yamlFilePath);
    if (infrastructureProvisioner != null) {
      infrastructureProvisionerService.delete(optionalApplication.get().getUuid(), infrastructureProvisioner.getUuid(),
          changeContext.getChange().isSyncFromGit());
    }
  }
}
