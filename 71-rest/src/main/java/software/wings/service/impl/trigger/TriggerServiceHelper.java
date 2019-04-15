package software.wings.service.impl.trigger;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static net.redhogs.cronparser.CronExpressionDescriptor.getDescription;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_COLLECTED;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_DEPLOYED;
import static software.wings.beans.trigger.TriggerConditionType.NEW_ARTIFACT;
import static software.wings.beans.trigger.TriggerConditionType.PIPELINE_COMPLETION;
import static software.wings.beans.trigger.TriggerConditionType.SCHEDULED;
import static software.wings.beans.trigger.TriggerConditionType.WEBHOOK;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import net.redhogs.cronparser.DescriptionTypeEnum;
import net.redhogs.cronparser.I18nMessages;
import net.redhogs.cronparser.Options;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.ServiceInfraWorkflow;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerConditionType;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.utils.CryptoUtil;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class TriggerServiceHelper {
  @Inject private WingsPersistence wingsPersistence;

  public void deletePipelineCompletionTriggers(String appId, String pipelineId) {
    getMatchedSourcePipelineTriggers(appId, pipelineId).collect(toList()).forEach(trigger -> delete(trigger.getUuid()));
  }

  public List<Trigger> getTriggersByApp(String appId) {
    return wingsPersistence.query(Trigger.class, aPageRequest().addFilter("appId", EQ, appId).build()).getResponse();
  }

  public boolean delete(String triggerId) {
    return wingsPersistence.delete(Trigger.class, triggerId);
  }

  public List<Trigger> getTriggersHasWorkflowAction(String appId, String workflowId) {
    return getTriggersByApp(appId)
        .stream()
        .filter(trigger
            -> trigger.getArtifactSelections().stream().anyMatch(artifactSelection
                -> artifactSelection.getType().equals(LAST_DEPLOYED)
                    && artifactSelection.getWorkflowId().equals(workflowId)))
        .collect(toList());
  }

  public List<Trigger> getTriggersHasArtifactStreamAction(String appId, String artifactStreamId) {
    return getTriggersByApp(appId)
        .stream()
        .filter(trigger
            -> trigger.getArtifactSelections().stream().anyMatch(artifactSelection
                -> artifactSelection.getType().equals(LAST_COLLECTED)
                    && artifactSelection.getArtifactStreamId().equals(artifactStreamId)))
        .collect(toList());
  }

  public List<Trigger> getNewInstanceTiggers(String appId) {
    return getTriggersByApp(appId)
        .stream()
        .filter(trigger -> trigger.getCondition().getConditionType().equals(TriggerConditionType.NEW_INSTANCE))
        .collect(toList());
  }

  public List<Trigger> getNewArtifactTriggers(String appId, String artifactStreamId) {
    return getTriggersByApp(appId)
        .stream()
        .filter(tr
            -> tr.getCondition().getConditionType().equals(NEW_ARTIFACT)
                && ((ArtifactTriggerCondition) tr.getCondition()).getArtifactStreamId().equals(artifactStreamId))
        .collect(toList());
  }

  public Trigger getTrigger(String appId, String webHookToken) {
    Trigger trigger = getTriggersByApp(appId)
                          .stream()
                          .filter(tr
                              -> tr.getCondition().getConditionType().equals(WEBHOOK)
                                  && ((WebHookTriggerCondition) tr.getCondition())
                                         .getWebHookToken()
                                         .getWebHookToken()
                                         .equals(webHookToken))
                          .findFirst()
                          .orElse(null);
    if (trigger == null) {
      throw new WingsException("Trigger does not exist or Invalid WebHook token", USER_ADMIN);
    }
    return trigger;
  }

  public List<Trigger> getTriggersMatchesWorkflow(String appId, String sourcePipelineId) {
    return getMatchedSourcePipelineTriggers(appId, sourcePipelineId)
        .filter(distinctByKey(Trigger::getWorkflowId))
        .collect(toList());
  }

  private Stream<Trigger> getMatchedSourcePipelineTriggers(String appId, String sourcePipelineId) {
    return getTriggersByApp(appId).stream().filter(trigger
        -> trigger.getCondition().getConditionType().equals(PIPELINE_COMPLETION)
            && ((PipelineTriggerCondition) trigger.getCondition()).getPipelineId().equals(sourcePipelineId));
  }

  public List<Trigger> getTriggersByWorkflow(String appId, String pipelineId) {
    return wingsPersistence.createQuery(Trigger.class)
        .filter(Trigger.APP_ID_KEY, appId)
        .filter("workflowId", pipelineId)
        .asList();
  }

  public List<String> checkTemplatedEntityReferenced(String appId, String envId) {
    List<String> referencedTriggers = new ArrayList<>();
    try (HIterator<Trigger> triggerHIterator =
             new HIterator<>(wingsPersistence.createQuery(Trigger.class).filter(APP_ID_KEY, appId).fetch())) {
      if (triggerHIterator != null) {
        while (triggerHIterator.hasNext()) {
          Trigger trigger = triggerHIterator.next();
          if (trigger.getWorkflowVariables() != null && trigger.getWorkflowVariables().values().contains(envId)) {
            referencedTriggers.add(trigger.getName());
          }
        }
      }
    }
    return referencedTriggers;
  }

  public List<ServiceInfraWorkflow> getServiceInfraWorkflows(String appId, String infraMappingId) {
    return getNewInstanceTiggers(appId)
        .stream()
        .filter(trigger -> trigger.getServiceInfraWorkflows() != null)
        .flatMap(trigger -> trigger.getServiceInfraWorkflows().stream())
        .filter(serviceInfraWorkflow
            -> serviceInfraWorkflow.getInfraMappingId() != null
                && serviceInfraWorkflow.getInfraMappingId().equals(infraMappingId))
        .filter(distinctByKey(ServiceInfraWorkflow::getWorkflowId))
        .collect(toList());
  }

  public static String getCronDescription(String cronExpression) {
    try {
      String description =
          getDescription(DescriptionTypeEnum.FULL, cronExpression, new Options(), I18nMessages.DEFAULT_LOCALE);
      return StringUtils.lowerCase("" + description.charAt(0)) + description.substring(1);
    } catch (Exception e) {
      throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "Invalid cron expression");
    }
  }

  public static void validateAndSetCronExpression(Trigger trigger) {
    if (trigger == null || !trigger.getCondition().getConditionType().equals(SCHEDULED)) {
      return;
    }
    ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
    try {
      if (isNotBlank(scheduledTriggerCondition.getCronExpression())) {
        CronScheduleBuilder.cronSchedule(ScheduledTriggerJob.PREFIX + scheduledTriggerCondition.getCronExpression());
        scheduledTriggerCondition.setCronDescription(
            getCronDescription(ScheduledTriggerJob.PREFIX + scheduledTriggerCondition.getCronExpression()));
      }
    } catch (Exception ex) {
      logger.warn("Error parsing cron expression: {} : {}", scheduledTriggerCondition.getCronExpression(),
          ExceptionUtils.getMessage(ex));
      throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "Invalid cron expression");
    }
  }

  public static WebHookToken constructWebhookToken(Trigger trigger, WebHookToken existingToken, List<Service> services,
      boolean artifactNeeded, Map<String, String> parameters) {
    WebHookToken webHookToken;
    if (existingToken == null || existingToken.getWebHookToken() == null) {
      webHookToken =
          WebHookToken.builder().httpMethod("POST").webHookToken(CryptoUtil.secureRandAlphaNumString(40)).build();
    } else {
      webHookToken = existingToken;
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("application", trigger.getAppId());

    List<Map<String, String>> artifactList = new ArrayList();
    if (isNotEmpty(trigger.getArtifactSelections())) {
      if (services != null) {
        for (Service service : services) {
          Map<String, String> artifacts = new HashMap<>();
          artifacts.put("service", service.getName());
          artifacts.put("buildNumber", service.getName() + "_BUILD_NUMBER_PLACE_HOLDER");
          artifactList.add(artifacts);
        }
      }
      if (!artifactList.isEmpty() && artifactNeeded) {
        payload.put("artifacts", artifactList);
      }
    }
    if (!parameters.isEmpty()) {
      payload.put("parameters", parameters);
    }
    webHookToken.setPayload(new Gson().toJson(payload));
    return webHookToken;
  }

  public static void notNullCheckWorkflow(Workflow workflow) {
    Validator.notNullCheck("Workflow was deleted", workflow, USER);
    Validator.notNullCheck("Orchestration workflow was deleted", workflow.getOrchestrationWorkflow(), USER);
  }

  public static void addParameter(List<String> parameters, List<Variable> variables, boolean includeEntityType) {
    if (isEmpty(variables)) {
      return;
    }

    List<Variable> filteredVariables = includeEntityType
        ? variables
        : variables.stream().filter(variable -> !variable.getType().equals(VariableType.ENTITY)).collect(toList());

    for (Variable userVariable : filteredVariables) {
      if (!parameters.contains(userVariable.getName())) {
        parameters.add(userVariable.getName());
      }
    }
  }

  public static List<String> obtainCollectedArtifactServiceIds(ExecutionArgs executionArgs) {
    List<String> artifactServiceIds = new ArrayList<>();
    final List<Artifact> artifacts = executionArgs.getArtifacts();
    if (isEmpty(artifacts)) {
      return new ArrayList<>();
    }
    artifacts.stream()
        .filter(artifact -> isNotEmpty(artifact.getServiceIds()))
        .map(Artifact::getServiceIds)
        .forEach(artifactServiceIds::addAll);
    return artifactServiceIds;
  }

  public static Map<String, String> overrideTriggerVariables(Trigger trigger, ExecutionArgs executionArgs) {
    // Workflow variables come from Webhook
    Map<String, String> webhookVariableValues =
        executionArgs.getWorkflowVariables() == null ? new HashMap<>() : executionArgs.getWorkflowVariables();

    // Workflow variables associated with the trigger
    Map<String, String> triggerWorkflowVariableValues =
        trigger.getWorkflowVariables() == null ? new HashMap<>() : trigger.getWorkflowVariables();

    for (Entry<String, String> entry : webhookVariableValues.entrySet()) {
      if (isNotEmpty(entry.getValue())) {
        triggerWorkflowVariableValues.put(entry.getKey(), entry.getValue());
      }
    }
    triggerWorkflowVariableValues = triggerWorkflowVariableValues.entrySet()
                                        .stream()
                                        .filter(variableEntry -> isNotEmpty(variableEntry.getValue()))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return triggerWorkflowVariableValues;
  }

  public boolean checkArtifactMatchesArtifactFilter(
      String triggerId, Artifact artifact, String artifactFilter, boolean isRegEx) {
    Pattern pattern;
    try {
      if (isRegEx) {
        pattern = compile(artifactFilter);
      } else {
        pattern = compile(artifactFilter.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
      }
    } catch (PatternSyntaxException pe) {
      logger.error("Invalid Build/Tag Filter {} for triggerId {}", artifactFilter, triggerId, pe);
      throw new WingsException("Invalid Build/Tag Filter", USER);
    }

    if (isEmpty(artifact.getArtifactFiles())) {
      if (pattern.matcher(artifact.getBuildNo()).find()) {
        logger.info(
            "Artifact filter {} matching with artifact name/ tag / buildNo {}", artifactFilter, artifact.getBuildNo());
        return true;
      }
    } else {
      logger.info("Comparing artifact file name matches with the given artifact filter");
      List<ArtifactFile> artifactFiles = artifact.getArtifactFiles()
                                             .stream()
                                             .filter(artifactFile -> pattern.matcher(artifactFile.getName()).find())
                                             .collect(toList());
      if (isNotEmpty(artifactFiles)) {
        logger.info("Artifact file names matches with the given artifact filter");
        artifact.setArtifactFiles(artifactFiles);
        return true;
      }
    }
    return false;
  }

  public <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
