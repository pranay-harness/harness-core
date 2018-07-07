package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.Workflow.LINKED_TEMPLATE_UUIDS_KEY;
import static software.wings.common.TemplateConstants.HTTP_HEALTH_CHECK;
import static software.wings.sm.StateType.HTTP;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.common.TemplateConstants;
import software.wings.dl.HIterator;
import software.wings.service.intfc.WorkflowService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class HttpTemplateProcessor extends AbstractTemplateProcessor {
  private static final Logger logger = LoggerFactory.getLogger(HttpTemplateProcessor.class);
  @Inject private WorkflowService workflowService;
  @Inject private TemplateHelper templateHelper;

  @Override
  public TemplateType getTemplateType() {
    return TemplateType.HTTP;
  }

  @Override
  public void loadDefaultTemplates(String accountId) {
    super.loadDefaultTemplates(Arrays.asList(HTTP_HEALTH_CHECK), accountId);
  }

  @Override
  public void updateLinkedEntities(Template template) {
    // Read http steps that references the given
    Template savedTemplate = templateService.get(template.getUuid());
    if (savedTemplate == null) {
      logger.info("Template {} was deleted. Not updating linked entities", template.getUuid());
      return;
    }
    try (HIterator<Workflow> workflowIterator = new HIterator<>(wingsPersistence.createQuery(Workflow.class)
                                                                    .field(LINKED_TEMPLATE_UUIDS_KEY)
                                                                    .contains(template.getUuid())
                                                                    .fetch())) {
      while (workflowIterator.hasNext()) {
        Workflow workflow = workflowIterator.next();
        try {
          workflow = workflowService.readWorkflow(workflow.getAppId(), workflow.getUuid());
          CanaryOrchestrationWorkflow orchestrationWorkflow =
              (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
          if (orchestrationWorkflow != null) {
            boolean updateNeeded = false;
            // Verify in pre-deployment steps
            updateNeeded = updateStep(template, updateNeeded, orchestrationWorkflow.getPostDeploymentSteps());
            // Verify in post deployment steps
            updateNeeded = updateStep(template, updateNeeded, orchestrationWorkflow.getPostDeploymentSteps());
            // Verify in phases
            List<WorkflowPhase> workflowPhases = orchestrationWorkflow.getWorkflowPhases();
            if (isNotEmpty(workflowPhases)) {
              for (WorkflowPhase workflowPhase : workflowPhases) {
                for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
                  updateNeeded = updateStep(template, updateNeeded, phaseStep);
                }
              }
            }
            if (updateNeeded) {
              workflowService.updateWorkflow(workflow);
            }
          }
        } catch (Exception e) {
          logger.warn("Error occurred while updating linked workflow {} ", workflow.getAppId(), e);
        }
      }
    }
  }

  private boolean updateStep(Template template, boolean updateNeeded, PhaseStep phaseStep) {
    if (phaseStep != null && phaseStep.getSteps() != null) {
      for (GraphNode step : phaseStep.getSteps()) {
        if (template.getUuid().equals(step.getTemplateUuid())
            && (step.getTemplateVersion() == null || TemplateConstants.LATEST_TAG.equals(step.getTemplateVersion()))) {
          GraphNode templateStep = constructEntityFromTemplate(template);
          step.setProperties(templateStep.getProperties());
          templateHelper.updateVariables(templateStep.getTemplateVariables(), step.getTemplateVariables(), true);
          step.setTemplateVariables(templateStep.getTemplateVariables());
          updateNeeded = true;
        }
      }
    }
    return updateNeeded;
  }

  @Override
  public GraphNode constructEntityFromTemplate(Template template) {
    HttpTemplate httpTemplate = (HttpTemplate) template.getTemplateObject();
    Map<String, Object> properties = new HashMap<>();
    transform(httpTemplate, properties);
    return aGraphNode()
        .withTemplateVariables(template.getVariables())
        .withProperties(properties)
        .withTemplateUuid(template.getUuid())
        .withType(HTTP.name())
        .build();
  }

  private void transform(HttpTemplate httpTemplate, Map<String, Object> properties) {
    if (isNotEmpty(httpTemplate.getUrl())) {
      properties.put("url", httpTemplate.getUrl());
    }
    if (isNotEmpty(httpTemplate.getMethod())) {
      properties.put("method", httpTemplate.getMethod());
    }
    if (isNotEmpty(httpTemplate.getHeader())) {
      properties.put("header", httpTemplate.getHeader());
    }
    if (isNotEmpty(httpTemplate.getBody())) {
      properties.put("body", httpTemplate.getBody());
    }
    if (isNotEmpty(httpTemplate.getAssertion())) {
      properties.put("assertion", httpTemplate.getAssertion());
    }
  }
}
