package software.wings.service.impl.yaml.handler;

import com.google.inject.Inject;

import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.app.ApplicationYamlHandler;
import software.wings.service.impl.yaml.handler.artifactstream.ArtifactStreamYamlHandler;
import software.wings.service.impl.yaml.handler.environment.EnvironmentYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.InfraMappingYamlHandler;
import software.wings.service.impl.yaml.handler.notification.NotificationGroupYamlHandler;
import software.wings.service.impl.yaml.handler.notification.NotificationRulesYamlHandler;
import software.wings.service.impl.yaml.handler.service.ServiceYamlHandler;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.handler.variable.VariableYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.FailureStrategyYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PhaseStepYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PipelineStageYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PipelineYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.StepYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowPhaseYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowYamlHandler;

import java.util.Map;

/**
 * @author rktummala on 10/19/17
 */
public class YamlHandlerFactory {
  @Inject private Map<String, ArtifactStreamYamlHandler> artifactStreamHelperMap;
  @Inject private Map<String, InfraMappingYamlHandler> infraMappingHelperMap;
  @Inject private Map<String, WorkflowYamlHandler> workflowYamlHelperMap;

  @Inject private ApplicationYamlHandler applicationYamlHandler;
  @Inject private EnvironmentYamlHandler environmentYamlHandler;
  @Inject private ServiceYamlHandler serviceYamlHandler;
  @Inject private NameValuePairYamlHandler nameValuePairYamlHandler;
  @Inject private PhaseStepYamlHandler phaseStepYamlHandler;
  @Inject private StepYamlHandler stepYamlHandler;
  @Inject private WorkflowPhaseYamlHandler workflowPhaseYamlHandler;
  @Inject private TemplateExpressionYamlHandler templateExpressionYamlHandler;
  @Inject private VariableYamlHandler variableYamlHandler;
  @Inject private NotificationRulesYamlHandler notificationRulesYamlHandler;
  @Inject private NotificationGroupYamlHandler notificationGroupYamlHandler;
  @Inject private FailureStrategyYamlHandler failureStrategyYamlHandler;
  @Inject private PipelineYamlHandler pipelineYamlHandler;
  @Inject private PipelineStageYamlHandler pipelineStageYamlHandler;

  // TODO change the return type to generics so that we don't have to explicitly downcast
  public BaseYamlHandler getYamlHandler(YamlType yamlType, String subType) {
    switch (yamlType) {
      case CLOUD_PROVIDER:
        break;
      case ARTIFACT_SERVER:
        // TODO
        break;
      case COLLABORATION_PROVIDER:
        // TODO
        break;
      case LOADBALANCER_PROVIDER:
        // TODO
        break;
      case VERIFICATION_PROVIDER:
        // TODO
        break;
      case APPLICATION:
        return applicationYamlHandler;
      case SERVICE:
        return serviceYamlHandler;
      case ARTIFACT_SOURCE:
        return artifactStreamHelperMap.get(subType);
      case COMMAND:
        break;
      case CONFIG_FILE:
        break;
      case ENVIRONMENT:
        return environmentYamlHandler;
      case CONFIG_FILE_OVERRIDE:
        break;
      case INFRA_MAPPING:
        return infraMappingHelperMap.get(subType);
      case PIPELINE:
        return pipelineYamlHandler;
      case PIPELINE_STAGE:
        return pipelineStageYamlHandler;
      case WORKFLOW:
        return workflowYamlHelperMap.get(subType);
      case NAME_VALUE_PAIR:
        return nameValuePairYamlHandler;
      case PHASE:
        return workflowPhaseYamlHandler;
      case PHASE_STEP:
        return phaseStepYamlHandler;
      case STEP:
        return stepYamlHandler;
      case TEMPLATE_EXPRESSION:
        return templateExpressionYamlHandler;
      case VARIABLE:
        return variableYamlHandler;
      case NOTIFICATION_RULE:
        return notificationRulesYamlHandler;
      case NOTIFICATION_GROUP:
        return notificationGroupYamlHandler;
      case FAILURE_STRATEGY:
        return failureStrategyYamlHandler;
      default:
        break;
    }

    return null;
  }
}
