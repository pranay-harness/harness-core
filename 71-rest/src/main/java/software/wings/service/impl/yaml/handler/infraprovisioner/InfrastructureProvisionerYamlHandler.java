package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.HarnessException;
import software.wings.beans.Application;
import software.wings.beans.BlueprintProperty;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisioner.InfraProvisionerYaml;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.NameValuePairYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Utils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class InfrastructureProvisionerYamlHandler<Y extends InfraProvisionerYaml, B
                                                               extends InfrastructureProvisioner>
    extends BaseYamlHandler<Y, B> {
  @Inject YamlHelper yamlHelper;
  @Inject InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject private AppService appService;
  @Inject private FeatureFlagService featureFlagService;

  private InfrastructureMappingBlueprint.Yaml mapBluePrintBeanToYaml(
      InfrastructureMappingBlueprint blueprint, String appId) {
    List<BlueprintProperty.Yaml> blueprintPropertiesYaml =
        blueprint.getProperties()
            .stream()
            .map(blueprintProperty -> {
              BlueprintProperty.Yaml blueprintPropertyYaml = BlueprintProperty.Yaml.builder()
                                                                 .name(blueprintProperty.getName())
                                                                 .value(blueprintProperty.getValue())
                                                                 .valueType(blueprintProperty.getValueType())
                                                                 .build();
              NameValuePairYamlHandler nameValuePairYamlHandler = getNameValuePairYamlHandler();

              List<NameValuePair.Yaml> fieldsYamls = Collections.emptyList();
              if (EmptyPredicate.isNotEmpty(blueprintProperty.getFields())) {
                fieldsYamls = blueprintProperty.getFields()
                                  .stream()
                                  .map(nameValuePair -> nameValuePairYamlHandler.toYaml(nameValuePair, appId))
                                  .collect(toList());
              }
              blueprintPropertyYaml.setFields(fieldsYamls);
              return blueprintPropertyYaml;
            })
            .collect(Collectors.toList());

    return InfrastructureMappingBlueprint.Yaml.builder()
        .cloudProviderType(blueprint.getCloudProviderType())
        .deploymentType(blueprint.getDeploymentType())
        .nodeFilteringType(blueprint.getNodeFilteringType())
        .serviceName(getServiceName(appId, blueprint.getServiceId()))
        .properties(blueprintPropertiesYaml)
        .build();
  }

  protected NameValuePairYamlHandler getNameValuePairYamlHandler() {
    return yamlHandlerFactory.getYamlHandler(YamlType.NAME_VALUE_PAIR);
  }

  public InfraProvisionerYaml toYaml(InfraProvisionerYaml yaml, InfrastructureProvisioner bean) {
    yaml.setDescription(bean.getDescription());
    yaml.setHarnessApiVersion(getHarnessApiVersion());

    String accountId = appService.getAccountIdByAppId(bean.getAppId());
    if (!featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId)
        && isNotEmpty(bean.getMappingBlueprints())) {
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

      yaml.setVariables(Utils.getSortedNameValuePairYamlList(nvpYamlList));
    }

    updateYamlWithAdditionalInfo(bean, bean.getAppId(), yaml);

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

    List<BlueprintProperty> blueprintProperties = null;
    if (isNotEmpty(yaml.getProperties())) {
      blueprintProperties =
          yaml.getProperties()
              .stream()
              .map(yamlProperty -> {
                BlueprintProperty blueprintProperty = BlueprintProperty.builder()
                                                          .name(yamlProperty.getName())
                                                          .value(yamlProperty.getValue())
                                                          .valueType(yamlProperty.getValueType())
                                                          .build();
                if (isNotEmpty(yamlProperty.getFields())) {
                  List<NameValuePair> fields =
                      yamlProperty.getFields()
                          .stream()
                          .map(nvpYaml
                              -> NameValuePair.builder().name(nvpYaml.getName()).value(nvpYaml.getValue()).build())
                          .collect(toList());
                  blueprintProperty.setFields(fields);
                }
                return blueprintProperty;
              })
              .collect(Collectors.toList());
    }

    return InfrastructureMappingBlueprint.builder()
        .cloudProviderType(yaml.getCloudProviderType())
        .deploymentType(yaml.getDeploymentType())
        .nodeFilteringType(yaml.getNodeFilteringType())
        .serviceId(serviceId)
        .properties(blueprintProperties)
        .build();
  }

  @Override
  public Class getYamlClass() {
    return InfraProvisionerYaml.class;
  }

  public void toBean(ChangeContext<Y> changeContext, B bean, String appId, String yamlPath) {
    Y yaml = changeContext.getYaml();

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    bean.setAppId(appId);
    bean.setName(name);
    bean.setDescription(yaml.getDescription());

    String accountId = appService.getAccountIdByAppId(appId);
    bean.setAccountId(accountId);

    if (!featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId)
        && isNotEmpty(yaml.getMappingBlueprints())) {
      List<InfrastructureMappingBlueprint> infrastructureMappingBlueprints =
          yaml.getMappingBlueprints()
              .stream()
              .map(bluePrint -> mapBluePrintYamlToBean(bluePrint, appId, yamlPath))
              .collect(toList());
      bean.setMappingBlueprints(infrastructureMappingBlueprints);
    }

    if (isNotEmpty(yaml.getVariables())) {
      List<NameValuePair> nameValuePairList = yaml.getVariables()
                                                  .stream()
                                                  .map(nvpYaml
                                                      -> NameValuePair.builder()
                                                             .name(nvpYaml.getName())
                                                             .value(nvpYaml.getValue())
                                                             .valueType(nvpYaml.getValueType())
                                                             .build())
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
