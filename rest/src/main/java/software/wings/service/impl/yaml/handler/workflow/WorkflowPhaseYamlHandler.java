package software.wings.service.impl.yaml.handler.workflow;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ObjectType;
import software.wings.beans.PhaseStep;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.Yaml;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Util;
import software.wings.utils.Validator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/27/17
 */
public class WorkflowPhaseYamlHandler extends BaseYamlHandler<WorkflowPhase.Yaml, WorkflowPhase> {
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject SettingsService settingsService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject YamlHandlerFactory yamlHandlerFactory;

  private WorkflowPhase toBean(ChangeContext<Yaml> context, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Yaml yaml = context.getYaml();
    Change change = context.getChange();

    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
    Validator.notNullCheck("Could not retrieve valid app from path: " + change.getFilePath(), appId);

    String envId = context.getEntityIdMap().get(EntityType.ENVIRONMENT.name());
    String infraMappingId = null;
    String infraMappingName = null;
    String deploymentTypeString = null;

    if (envId != null) {
      InfrastructureMapping infraMapping =
          infraMappingService.getInfraMappingByName(appId, envId, yaml.getInfraMappingName());
      infraMappingId = infraMapping != null ? infraMapping.getUuid() : null;
      infraMappingName = infraMapping != null ? infraMapping.getName() : null;
      deploymentTypeString = infraMapping != null ? infraMapping.getDeploymentType() : null;
    }

    Service service = serviceResourceService.getServiceByName(appId, yaml.getServiceName());
    String serviceId = service != null ? service.getUuid() : null;

    // phase step
    List<PhaseStep> phaseSteps = Lists.newArrayList();
    if (yaml.getPhaseSteps() != null) {
      BaseYamlHandler phaseStepYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.PHASE_STEP, ObjectType.PHASE_STEP);
      phaseSteps =
          yaml.getPhaseSteps()
              .stream()
              .map(phaseStep -> {
                try {
                  ChangeContext.Builder clonedContext = cloneFileChangeContext(context, phaseStep);
                  return (PhaseStep) phaseStepYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
                } catch (HarnessException e) {
                  throw new WingsException(e);
                }
              })
              .collect(Collectors.toList());
    }

    // template expressions
    List<TemplateExpression> templateExpressions = Lists.newArrayList();
    if (yaml.getTemplateExpressions() != null) {
      BaseYamlHandler templateExprYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION, ObjectType.TEMPLATE_EXPRESSION);
      templateExpressions = yaml.getTemplateExpressions()
                                .stream()
                                .map(templateExpr -> {
                                  try {
                                    ChangeContext.Builder clonedContext = cloneFileChangeContext(context, templateExpr);
                                    return (TemplateExpression) templateExprYamlHandler.upsertFromYaml(
                                        clonedContext.build(), changeSetContext);
                                  } catch (HarnessException e) {
                                    throw new WingsException(e);
                                  }
                                })
                                .collect(Collectors.toList());
    }

    SettingAttribute computeProvider = settingsService.getByName(appId, yaml.getComputeProviderName());
    String computeProviderId;
    if (computeProvider == null) {
      computeProviderId = yaml.getComputeProviderName();
    } else {
      computeProviderId = computeProvider.getUuid();
    }

    WorkflowPhase.WorkflowPhaseBuilder phase = WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase();
    DeploymentType deploymentType = Util.getEnumFromString(DeploymentType.class, deploymentTypeString);
    phase.withComputeProviderId(computeProviderId)
        .withDeploymentType(deploymentType)
        .withInfraMappingId(infraMappingId)
        .withInfraMappingName(infraMappingName)
        .withName(yaml.getName())
        .withPhaseNameForRollback(yaml.getPhaseNameForRollback())
        .withPhaseSteps(phaseSteps)
        .withServiceId(serviceId)
        .withTemplateExpressions(templateExpressions)
        .build();
    return phase.build();
  }

  @Override
  public Yaml toYaml(WorkflowPhase bean, String appId) {
    SettingAttribute settingAttribute = settingsService.get(bean.getComputeProviderId());
    String computeProviderName;
    if (settingAttribute == null) {
      computeProviderName = bean.getComputeProviderId();
    } else {
      computeProviderName = settingAttribute.getName();
    }

    // template expressions
    List<TemplateExpression.Yaml> templateExprYamlList = Lists.newArrayList();
    if (bean.getTemplateExpressions() != null) {
      BaseYamlHandler templateExpressionYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION, ObjectType.TEMPLATE_EXPRESSION);
      templateExprYamlList =
          bean.getTemplateExpressions()
              .stream()
              .map(templateExpression
                  -> (TemplateExpression.Yaml) templateExpressionYamlHandler.toYaml(templateExpression, appId))
              .collect(Collectors.toList());
    }

    List<PhaseStep.Yaml> phaseStepYamlList = Lists.newArrayList();
    if (bean.getPhaseSteps() != null) {
      BaseYamlHandler phaseStepYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.PHASE_STEP, ObjectType.PHASE_STEP);
      phaseStepYamlList = bean.getPhaseSteps()
                              .stream()
                              .map(phaseStep -> (PhaseStep.Yaml) phaseStepYamlHandler.toYaml(phaseStep, appId))
                              .collect(Collectors.toList());
    }

    Service service = serviceResourceService.get(appId, bean.getServiceId());
    String serviceName = service != null ? service.getName() : null;
    String deploymentType = Util.getStringFromEnum(bean.getDeploymentType());

    String infraMappingId = bean.getInfraMappingId();
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    Validator.notNullCheck("Infra mapping not found for given id:" + infraMappingId, infrastructureMapping);

    return Yaml.builder()
        .computeProviderName(computeProviderName)
        .infraMappingName(infrastructureMapping.getName())
        .serviceName(serviceName)
        .name(bean.getName())
        .phaseNameForRollback(bean.getPhaseNameForRollback())
        .phaseSteps(phaseStepYamlList)
        .provisionNodes(bean.isProvisionNodes())
        .templateExpressions(templateExprYamlList)
        .type(deploymentType)
        .build();
  }

  @Override
  public WorkflowPhase upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext, changeSetContext);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return WorkflowPhase.Yaml.class;
  }

  @Override
  public WorkflowPhase get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
