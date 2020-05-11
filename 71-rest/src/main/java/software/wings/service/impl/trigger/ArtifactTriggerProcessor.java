package software.wings.service.impl.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.trigger.Condition.Type.NEW_ARTIFACT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.ArtifactTriggerCondition.ArtifactTriggerConditionKeys;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.DeploymentTrigger.DeploymentTriggerKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class ArtifactTriggerProcessor implements TriggerProcessor {
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private transient DeploymentTriggerServiceHelper triggerServiceHelper;
  @Inject private transient TriggerArtifactVariableHandler triggerArtifactVariableHandler;
  @Inject private transient TriggerDeploymentExecution triggerDeploymentExecution;
  @Inject private transient ExecutorService executorService;
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient FeatureFlagService featureFlagService;
  @Inject private transient AppService appService;
  @Inject private transient SettingsService settingsService;

  @Override
  public void validateTriggerConditionSetup(DeploymentTrigger trigger, DeploymentTrigger existingTrigger) {
    // TODO: ASR: update when index added on setting_id + name
    ArtifactCondition artifactCondition = (ArtifactCondition) trigger.getCondition();
    notNullCheck("Artifact stream Id is null in condition for trigger: " + trigger.getName(),
        artifactCondition.getArtifactStreamId());
    ArtifactStream artifactStream = artifactStreamService.get(artifactCondition.getArtifactStreamId());
    notNullCheck("Artifact Source is mandatory for New Artifact Condition Trigger", artifactStream, USER);

    validateArtifactFilter(artifactCondition.getArtifactFilter());
    trigger.setCondition(ArtifactCondition.builder()
                             .artifactStreamId(artifactCondition.getArtifactStreamId())
                             .artifactServerId(artifactStream.getSettingId())
                             .artifactFilter(artifactCondition.getArtifactFilter())
                             .build());
  }

  @Override
  public void validateTriggerActionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    triggerServiceHelper.validateTriggerAction(deploymentTrigger);
  }

  @Override
  public void transformTriggerConditionRead(DeploymentTrigger deploymentTrigger) {
    ArtifactCondition artifactCondition = (ArtifactCondition) deploymentTrigger.getCondition();
    ArtifactStream artifactStream = artifactStreamService.get(artifactCondition.getArtifactStreamId());

    SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());

    if (settingAttribute == null) {
      throw new WingsException(
          format("Unable to find Connector/Cloud Provider for artifact stream %s", artifactStream.getSourceName()),
          WingsException.USER);
    }
    deploymentTrigger.setCondition(ArtifactCondition.builder()
                                       .artifactStreamId(artifactCondition.getArtifactStreamId())
                                       .artifactServerId(artifactCondition.getArtifactServerId())
                                       .artifactStreamName(artifactStream.getName())
                                       .artifactServerName(settingAttribute.getName())
                                       .artifactStreamType(artifactStream.getArtifactStreamType())
                                       .artifactFilter(artifactCondition.getArtifactFilter())
                                       .build());
  }

  @Override
  public void transformTriggerActionRead(DeploymentTrigger deploymentTrigger, boolean readPrimaryVariablesValueNames) {
    triggerServiceHelper.reBuildTriggerActionWithNames(deploymentTrigger, readPrimaryVariablesValueNames);
  }

  @Override
  public WorkflowExecution executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams) {
    ArtifactTriggerExecutionParams artifactTriggerExecutionParams =
        (ArtifactTriggerExecutionParams) triggerExecutionParams;

    triggerExecutionPostArtifactCollection(
        appId, artifactTriggerExecutionParams.artifactStreamId, artifactTriggerExecutionParams.collectedArtifacts);

    return null;
  }

  private void validateArtifactFilter(String artifactFilter) {
    if (artifactFilter != null) {
      try {
        compile(artifactFilter);
      } catch (PatternSyntaxException pe) {
        throw new WingsException("Invalid Build/Tag Filter, Please provide a valid regex", USER);
      }
    }
  }

  private List<DeploymentTrigger> fetchNewArtifactTriggers(String appId, String artifactStreamId) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return fetchNewArtifactTriggersForAllApps(artifactStreamId);
    }

    return triggerServiceHelper.getTriggersByApp(appId, NEW_ARTIFACT)
        .stream()
        .filter(tr -> ((ArtifactCondition) tr.getCondition()).getArtifactStreamId().equals(artifactStreamId))
        .collect(toList());
  }

  private List<DeploymentTrigger> fetchNewArtifactTriggersForAllApps(String artifactStreamId) {
    return wingsPersistence.createQuery(DeploymentTrigger.class, excludeAuthority)
        .disableValidation()
        .filter(DeploymentTriggerKeys.condition + "."
                + "type",
            NEW_ARTIFACT)
        .filter(DeploymentTriggerKeys.condition + "." + ArtifactTriggerConditionKeys.artifactStreamId, artifactStreamId)
        .asList();
  }

  private void triggerExecutionPostArtifactCollection(
      String appId, String artifactStreamId, List<Artifact> collectedArtifacts) {
    executorService.execute(() -> executeTrigger(appId, artifactStreamId, collectedArtifacts));
  }

  private void executeTrigger(String appId, String artifactStreamId, List<Artifact> collectedArtifacts) {
    fetchNewArtifactTriggers(appId, artifactStreamId).forEach(trigger -> {
      if (trigger.isTriggerDisabled()) {
        logger.warn("Trigger is disabled for appId {}, Trigger Id {} and name {} for artifactStreamId {} ",
            trigger.getAppId(), trigger.getUuid(), artifactStreamId);
        return;
      }
      logger.info("Trigger found with name {} and Id {} for artifactStreamId {}", trigger.getName(), trigger.getUuid(),
          artifactStreamId);
      ArtifactCondition artifactTriggerCondition = (ArtifactCondition) trigger.getCondition();
      List<Artifact> artifacts = new ArrayList<>();
      if (isEmpty(artifactTriggerCondition.getArtifactFilter())) {
        logger.info("No artifact filter set. Triggering with the collected artifacts");
        addArtifactsToBeProcess(trigger.getAppId(), artifacts, collectedArtifacts);
      } else {
        logger.info("Artifact filter is set. Triggering with the filtered artifacts");
        addArtifactForRegex(trigger, collectedArtifacts, artifactTriggerCondition, artifacts);
      }
      if (isEmpty(artifacts)) {
        logger.warn(
            "Skipping execution - artifact does not match with the given filter. So, skipping the complete deployment {}",
            artifactTriggerCondition);
        return;
      }

      String accountId = appService.getAccountIdByAppId(trigger.getAppId());
      if (featureFlagService.isEnabled(FeatureName.TRIGGER_FOR_ALL_ARTIFACTS, accountId)) {
        for (Artifact artifact : artifacts) {
          List<Artifact> individualArtifact = new ArrayList();
          individualArtifact.add(artifact);
          executeDeployment(trigger, individualArtifact, artifactStreamId);
        }
      } else {
        executeDeployment(trigger, artifacts, artifactStreamId);
      }
    });
  }

  private void addArtifactForRegex(DeploymentTrigger trigger, List<Artifact> collectedArtifacts,
      ArtifactCondition artifactTriggerCondition, List<Artifact> artifacts) {
    logger.info("Artifact filter {} set. Going over all the artifacts to find the matched artifacts",
        artifactTriggerCondition.getArtifactFilter());
    List<Artifact> matchedArtifacts = collectedArtifacts.stream()
                                          .filter(artifact
                                              -> checkArtifactMatchesArtifactFilter(trigger.getUuid(), artifact,
                                                  artifactTriggerCondition.getArtifactFilter()))
                                          .collect(Collectors.toList());
    if (isNotEmpty(matchedArtifacts)) {
      addArtifactsToBeProcess(trigger.getAppId(), artifacts, matchedArtifacts);
    } else {
      logger.info("Artifacts {} not matched with the given artifact filter", artifacts);
    }
  }

  private boolean checkArtifactMatchesArtifactFilter(String triggerId, Artifact artifact, String artifactFilter) {
    Pattern pattern;
    try {
      pattern = compile(artifactFilter);
    } catch (PatternSyntaxException pe) {
      logger.warn("Invalid Build/Tag Filter {} for triggerId {}", artifactFilter, triggerId, pe);
      throw new WingsException("Invalid Build/Tag Filter", USER);
    }

    if (!isEmpty(artifact.getArtifactFiles())) {
      logger.info("Comparing artifact file name matches with the given artifact filter");
      List<ArtifactFile> artifactFiles = artifact.getArtifactFiles()
                                             .stream()
                                             .filter(artifactFile -> pattern.matcher(artifactFile.getName()).find())
                                             .collect(toList());
      if (isNotEmpty(artifactFiles)) {
        logger.info("Artifact file names matches with the given artifact filter {}", triggerId);
        artifact.setArtifactFiles(artifactFiles);
        return true;
      }

    } else {
      if (pattern.matcher(artifact.getBuildNo()).find()) {
        logger.info(
            "Artifact filter {} matching with artifact name/ tag / buildNo {}", artifactFilter, artifact.getBuildNo());
        return true;
      }
    }
    return false;
  }

  private void addArtifactsToBeProcess(String appId, List<Artifact> artifacts, List<Artifact> inputArtifacts) {
    String accountId = appService.getAccountIdByAppId(appId);
    if (!featureFlagService.isEnabled(FeatureName.TRIGGER_FOR_ALL_ARTIFACTS, accountId)) {
      artifacts.add(inputArtifacts.get(inputArtifacts.size() - 1));
      logger.info("Selecting the latest artifact", inputArtifacts.get(inputArtifacts.size() - 1));
    } else {
      artifacts.addAll(inputArtifacts);
      logger.info("Triggering with all artifacts as TRIGGER_FOR_ALL_ARTIFACTS is enabled");
    }
  }

  private void executeDeployment(DeploymentTrigger trigger, List<Artifact> artifacts, String artifactStreamId) {
    if (trigger.getAction() != null) {
      logger.info("Artifact selections found collecting artifacts as per artifactStream selections");

      // Fetch and validate artifact variables
      List<ArtifactVariable> artifactVariables =
          triggerArtifactVariableHandler.fetchArtifactVariablesForExecution(trigger.getAppId(), trigger, artifacts);

      if (isNotEmpty(artifactVariables)) {
        logger.info("The artifact variables  set for the trigger {} are {}", trigger.getUuid(),
            artifactVariables.stream().map(ArtifactVariable::getName).collect(toList()));
      }
      try {
        triggerDeploymentExecution.triggerDeployment(artifactVariables, null, trigger, null);
      } catch (WingsException exception) {
        exception.addContext(Application.class, trigger.getAppId());
        exception.addContext(ArtifactStream.class, artifactStreamId);
        exception.addContext(DeploymentTrigger.class, trigger.getUuid());
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      }
    } else {
      logger.info("No action exist. Hence Skipping the deployment");
    }
  }

  @Value
  @Builder
  public static class ArtifactTriggerExecutionParams implements TriggerExecutionParams {
    String artifactStreamId;
    List<Artifact> collectedArtifacts;
  }
}