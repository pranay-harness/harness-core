package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PhaseStep;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.beans.WorkflowPhase.Yaml;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Util;

import java.util.List;

/**
 * @author rktummala on 10/27/17
 */
@Singleton
public class WorkflowPhaseYamlHandler extends BaseYamlHandler<WorkflowPhase.Yaml, WorkflowPhase> {
  @Inject private YamlHelper yamlHelper;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SettingsService settingsService;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;

  private WorkflowPhase toBean(ChangeContext<Yaml> context, List<ChangeContext> changeSetContext) {
    Yaml yaml = context.getYaml();
    Change change = context.getChange();
    String accountId = change.getAccountId();
    String appId = yamlHelper.getAppId(accountId, change.getFilePath());
    notNullCheck("Could not retrieve valid app from path: " + change.getFilePath(), appId, USER);

    String envId = context.getEntityIdMap().get(EntityType.ENVIRONMENT.name());
    String infraMappingId = null;
    String infraMappingName = null;
    // @Todo read from yaml itslf rather than infraMapping
    String deploymentTypeString = yaml.getType();

    if (envId != null && isNotEmpty(yaml.getInfraMappingName())) {
      InfrastructureMapping infraMapping =
          infraMappingService.getInfraMappingByName(appId, envId, yaml.getInfraMappingName());
      infraMappingId = infraMapping != null ? infraMapping.getUuid() : null;
      infraMappingName = infraMapping != null ? infraMapping.getName() : null;
      // deploymentTypeString = infraMapping != null ? infraMapping.getDeploymentType() : null;
    }

    String serviceId = null;
    if (isNotEmpty(yaml.getServiceName())) {
      Service service = serviceResourceService.getServiceByName(appId, yaml.getServiceName());
      serviceId = service != null ? service.getUuid() : null;
    }

    // phase step
    List<PhaseStep> phaseSteps = Lists.newArrayList();
    if (yaml.getPhaseSteps() != null) {
      PhaseStepYamlHandler phaseStepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.PHASE_STEP);
      phaseSteps = yaml.getPhaseSteps()
                       .stream()
                       .map(phaseStep -> {
                         try {
                           ChangeContext.Builder clonedContext = cloneFileChangeContext(context, phaseStep);
                           return phaseStepYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
                         } catch (HarnessException e) {
                           throw new WingsException(e);
                         }
                       })
                       .collect(toList());
    }

    // template expressions
    List<TemplateExpression> templateExpressions = Lists.newArrayList();
    if (yaml.getTemplateExpressions() != null) {
      TemplateExpressionYamlHandler templateExprYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION);
      templateExpressions =
          yaml.getTemplateExpressions()
              .stream()
              .map(templateExpr -> {
                try {
                  ChangeContext.Builder clonedContext = cloneFileChangeContext(context, templateExpr);
                  return templateExprYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
                } catch (HarnessException e) {
                  throw new WingsException(e);
                }
              })
              .collect(toList());
    }

    SettingAttribute computeProvider = settingsService.getByName(accountId, appId, yaml.getComputeProviderName());
    String computeProviderId;
    if (computeProvider == null) {
      computeProviderId = yaml.getComputeProviderName();
    } else {
      computeProviderId = computeProvider.getUuid();
    }

    Boolean isRollback = (Boolean) context.getProperties().get(YamlConstants.IS_ROLLBACK);
    WorkflowPhaseBuilder phase = WorkflowPhaseBuilder.aWorkflowPhase();
    DeploymentType deploymentType = Util.getEnumFromString(DeploymentType.class, deploymentTypeString);
    phase.withComputeProviderId(computeProviderId)
        .withDeploymentType(deploymentType)
        .withInfraMappingId(infraMappingId)
        .withInfraMappingName(infraMappingName)
        .withName(yaml.getName())
        .withPhaseNameForRollback(yaml.getPhaseNameForRollback())
        .withPhaseSteps(phaseSteps)
        .withServiceId(serviceId)
        .withRollback(isRollback)
        .withTemplateExpressions(templateExpressions)
        .withDaemonSet(yaml.isDaemonSet())
        .withStatefulSet(yaml.isStatefulSet())
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
      TemplateExpressionYamlHandler templateExpressionYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION);
      templateExprYamlList =
          bean.getTemplateExpressions()
              .stream()
              .map(templateExpression -> templateExpressionYamlHandler.toYaml(templateExpression, appId))
              .collect(toList());
    }

    List<PhaseStep.Yaml> phaseStepYamlList = Lists.newArrayList();
    if (bean.getPhaseSteps() != null) {
      PhaseStepYamlHandler phaseStepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.PHASE_STEP);
      phaseStepYamlList = bean.getPhaseSteps()
                              .stream()
                              .map(phaseStep -> phaseStepYamlHandler.toYaml(phaseStep, appId))
                              .collect(toList());
    }

    String deploymentType = Util.getStringFromEnum(bean.getDeploymentType());
    String serviceName = null;
    if (isNotEmpty(bean.getServiceId())) {
      Service service = serviceResourceService.get(appId, bean.getServiceId());
      serviceName = service != null ? service.getName() : null;
    }

    String infraMappingName = null;
    String infraMappingId = bean.getInfraMappingId();
    if (isNotEmpty(infraMappingId)) {
      InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);

      // dont set infraName is its templatized
      if (infrastructureMapping != null) {
        infraMappingName = infrastructureMapping.getName();
      }

      // when templatized infraMappings used, we do expect infraMapping can be null, so don't perform this check
      if (infrastructureMapping == null && !bean.checkInfraTemplatized()) {
        String message = format("Infra-mapping:%s could not be found for workflowPhase:%s, for app:%s", infraMappingId,
            bean.getName(), appId);
        throw new WingsException(ErrorCode.GENERAL_ERROR, USER).addParam("message", message);
      }
    }

    return Yaml.builder()
        .computeProviderName(computeProviderName)
        .infraMappingName(infraMappingName)
        .serviceName(serviceName)
        .name(bean.getName())
        .phaseNameForRollback(bean.getPhaseNameForRollback())
        .phaseSteps(phaseStepYamlList)
        .provisionNodes(bean.isProvisionNodes())
        .templateExpressions(templateExprYamlList)
        .type(deploymentType)
        .daemonSet(bean.isDaemonSet())
        .statefulSet(bean.isStatefulSet())
        .build();
  }

  @Override
  public WorkflowPhase upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext, changeSetContext);
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
