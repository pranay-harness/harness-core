package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static software.wings.beans.Base.LINKED_TEMPLATE_UUIDS_KEY;

import com.google.inject.Inject;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import io.harness.persistence.HIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateHelper;
import software.wings.common.TemplateConstants;
import software.wings.service.intfc.WorkflowService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class StateTemplateProcessor extends AbstractTemplateProcessor {
  private static final Logger logger = LoggerFactory.getLogger(StateTemplateProcessor.class);
  @Inject private WorkflowService workflowService;
  @Inject private TemplateHelper templateHelper;

  @Override
  public void updateLinkedEntities(Template template) {
    // Read steps that references the given template
    Template savedTemplate = templateService.get(template.getUuid());
    if (savedTemplate == null) {
      logger.info("Template {} was deleted. Not updating linked entities", template.getUuid());
      return;
    }
    try (HIterator<Workflow> workflowIterator =
             new HIterator<>(wingsPersistence.createQuery(Workflow.class, excludeAuthority)
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
            updateNeeded = updateStep(template, updateNeeded, orchestrationWorkflow.getPreDeploymentSteps());
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
          logger.warn(format("Error occurred while updating linked workflow %s", workflow.getUuid()), e);
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
          Map<String, Object> stepProperties = step.getProperties();
          if (templateStep != null) {
            stepProperties.putAll(templateStep.getProperties());
          }
          step.setProperties(stepProperties);
          step.setTemplateVariables(
              templateHelper.overrideVariables(templateStep.getTemplateVariables(), step.getTemplateVariables()));
          updateNeeded = true;
        }
      }
    }
    return updateNeeded;
  }

  @Override
  public GraphNode constructEntityFromTemplate(Template template) {
    Map<String, Object> properties = new HashMap<>();
    transform(template, properties);
    return GraphNode.builder()
        .templateVariables(template.getVariables())
        .properties(properties)
        .templateUuid(template.getUuid())
        .type(getTemplateType().name())
        .build();
  }

  public abstract void transform(Template template, Map<String, Object> properties);

  @Override
  public boolean checkTemplateDetailsChanged(BaseTemplate oldTemplate, BaseTemplate newTemplate) {
    DiffNode templateDetailsDiff = ObjectDifferBuilder.buildDefault().compare(newTemplate, oldTemplate);
    return templateDetailsDiff.hasChanges();
  }
}
