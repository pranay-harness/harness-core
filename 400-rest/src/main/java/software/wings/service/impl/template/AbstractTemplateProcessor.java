/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.LATEST_TAG;

import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.ImportedTemplateMetadata;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateHelper;
import software.wings.common.TemplateConstants;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.template.TemplateService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractTemplateProcessor {
  @Inject protected TemplateService templateService;
  @Inject protected WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;
  @Inject private TemplateHelper templateHelper;

  ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  /**
   * Process the template
   *
   * @param template
   */
  public Template process(Template template) {
    template.setType(getTemplateType().name());
    return template;
  }

  public abstract software.wings.beans.template.TemplateType getTemplateType();

  /**
   * Loads Harness default command templates
   *
   * @param accountId
   */
  public void loadDefaultTemplates(String accountId, String accountName) {}

  public void loadDefaultTemplates(List<String> templateFiles, String accountId, String accountName) {
    // First
    templateFiles.forEach(templatePath -> {
      try {
        log.info("Loading url file {} for the account {} ", templatePath, accountId);
        loadAndSaveTemplate(templatePath, accountId, accountName);
      } catch (WingsException exception) {
        String msg = "Failed to save template from file [" + templatePath + "] for the account [" + accountId
            + "] . Reason:" + exception.getMessage();
        throw new WingsException(msg, exception, WingsException.USER);
      } catch (IOException exception) {
        String msg = "Failed to save template from file [" + templatePath + "]. Reason:" + exception.getMessage();
        throw new WingsException(msg, exception, WingsException.USER);
      }
    });
  }

  /**
   * Loads Yaml file and returns Template
   *
   * @param templatePath
   * @return
   */
  public Template loadYaml(String templatePath, String accountId, String accountName) {
    try {
      return loadAndSaveTemplate(templatePath, accountId, accountName);
    } catch (IOException e) {
      throw new WingsException("Failed to load template from path " + templatePath, WingsException.SRE);
    }
  }

  private Template loadAndSaveTemplate(String templatePath, String accountId, String accountName) throws IOException {
    URL url = this.getClass().getClassLoader().getResource(templatePath);
    Template template = mapper.readValue(url, Template.class);

    if (!GLOBAL_ACCOUNT_ID.equals(accountId)) {
      String referencedTemplateUri = template.getReferencedTemplateUri();
      if (isNotEmpty(referencedTemplateUri)) {
        String referencedTemplateVersion = TemplateHelper.obtainTemplateVersion(referencedTemplateUri);
        template.setReferencedTemplateId(
            templateService.fetchTemplateIdFromUri(GLOBAL_ACCOUNT_ID, referencedTemplateUri));
        if (!LATEST_TAG.equals(referencedTemplateVersion)) {
          if (referencedTemplateVersion != null) {
            template.setReferencedTemplateVersion(Long.valueOf(referencedTemplateVersion));
          }
        }
      }
      if (isNotEmpty(template.getFolderPath())) {
        template.setFolderPath(template.getFolderPath().replace(HARNESS_GALLERY, accountName));
      }
    }
    template.setAppId(GLOBAL_APP_ID);
    template.setAccountId(accountId);
    return templateService.save(template);
  }

  public abstract void updateLinkedEntities(Template template);

  public abstract Object constructEntityFromTemplate(Template template, EntityType entityType);

  public abstract List<String> fetchTemplateProperties();

  public abstract boolean checkTemplateDetailsChanged(BaseTemplate oldTemplate, BaseTemplate newTemplate);

  public void updateLinkedEntitiesInWorkflow(Template template) {
    try (HIterator<Workflow> workflowIterator =
             new HIterator<>(wingsPersistence.createQuery(Workflow.class, excludeAuthority)
                                 .filter(WorkflowKeys.linkedTemplateUuids, template.getUuid())
                                 .fetch())) {
      for (Workflow workflow : workflowIterator) {
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
            // Update Rollback Phase Steps
            if (orchestrationWorkflow.getRollbackWorkflowPhaseIdMap() != null) {
              for (WorkflowPhase workflowPhase : orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().values()) {
                if (isNotEmpty(workflowPhase.getPhaseSteps())) {
                  for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
                    updateNeeded = updateStep(template, updateNeeded, phaseStep);
                  }
                }
              }
            }
            if (updateNeeded) {
              workflowService.updateWorkflow(workflow, false);
            }
          }
        } catch (Exception e) {
          log.warn("Error occurred while updating linked workflow {}", workflow.getUuid(), e);
        }
      }
    }
  }

  private boolean updateStep(Template template, boolean updateNeeded, PhaseStep phaseStep) {
    if (phaseStep != null && phaseStep.getSteps() != null) {
      for (GraphNode step : phaseStep.getSteps()) {
        if (template.getUuid().equals(step.getTemplateUuid())
            && ((step.getTemplateVersion() == null || TemplateConstants.LATEST_TAG.equals(step.getTemplateVersion()))
                || ((TemplateConstants.DEFAULT_TAG.equals(step.getTemplateVersion()))
                    && isDefaultVersionOfTemplateChanged(template)))) {
          GraphNode templateStep = (GraphNode) constructEntityFromTemplate(template, EntityType.WORKFLOW);
          step.setTemplateVariables(
              templateHelper.overrideVariables(templateStep.getTemplateVariables(), step.getTemplateVariables()));
          step.setImportedTemplateDetails(
              TemplateHelper.getImportedTemplateDetails(template, step.getTemplateVersion()));
          step.setTemplateMetadata(template.getTemplateMetadata());
          updateNeeded = true;
        }
      }
    }
    return updateNeeded;
  }

  public boolean isDefaultVersionOfTemplateChanged(Template template) {
    if (template.getTemplateMetadata() != null && template.getTemplateMetadata() instanceof ImportedTemplateMetadata) {
      ImportedTemplateMetadata importedTemplateMetadata = (ImportedTemplateMetadata) template.getTemplateMetadata();
      if (importedTemplateMetadata.getDefaultVersion() != null) {
        return true;
      }
    }
    return false;
  }
}
