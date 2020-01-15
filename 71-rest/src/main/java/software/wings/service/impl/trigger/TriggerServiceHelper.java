package software.wings.service.impl.trigger;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_COLLECTED;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_DEPLOYED;
import static software.wings.beans.trigger.TriggerConditionType.NEW_ARTIFACT;
import static software.wings.beans.trigger.TriggerConditionType.PIPELINE_COMPLETION;
import static software.wings.beans.trigger.TriggerConditionType.SCHEDULED;
import static software.wings.beans.trigger.TriggerConditionType.WEBHOOK;
import static software.wings.scheduler.ScheduledTriggerJob.PREFIX;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import net.redhogs.cronparser.I18nMessages;
import org.quartz.CronScheduleBuilder;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.trigger.ArtifactSelection.ArtifactSelectionKeys;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.ArtifactTriggerCondition.ArtifactTriggerConditionKeys;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.ServiceInfraWorkflow;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.beans.trigger.TriggerCondition.TriggerConditionKeys;
import software.wings.beans.trigger.TriggerConditionType;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.utils.CryptoUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  public void deletePipelineCompletionTriggers(String appId, String pipelineId) {
    getMatchedSourcePipelineTriggers(appId, pipelineId).collect(toList()).forEach(trigger -> delete(trigger.getUuid()));
  }

  public List<Trigger> getTriggersByApp(String appId) {
    return wingsPersistence.query(Trigger.class, aPageRequest().addFilter("appId", EQ, appId).build()).getResponse();
  }

  public List<Trigger> getTriggersByArtifactStream(String artifactStreamId) {
    List<Trigger> triggers = getNewArtifactTriggers(artifactStreamId);
    Set<String> triggerIds = triggers.stream().map(Trigger::getUuid).collect(Collectors.toSet());
    getTriggersByArtifactStreamSelection(artifactStreamId).forEach(trigger -> {
      if (!triggerIds.contains(trigger.getUuid())) {
        triggerIds.add(trigger.getUuid());
        triggers.add(trigger);
      }
    });
    return triggers;
  }

  public List<Trigger> getTriggersByArtifactStreamSelection(String artifactStreamId) {
    return wingsPersistence.createQuery(Trigger.class, excludeAuthority)
        .disableValidation()
        .filter(TriggerKeys.artifactSelections + "." + ArtifactSelectionKeys.artifactStreamId, artifactStreamId)
        .asList();
  }

  public boolean delete(String triggerId) {
    return wingsPersistence.delete(Trigger.class, triggerId);
  }

  public List<Trigger> getTriggersHasWorkflowAction(String appId, String workflowId) {
    return getTriggersByApp(appId)
        .stream()
        .filter(trigger
            -> trigger.getArtifactSelections().stream().anyMatch(artifactSelection
                -> artifactSelection.getType() == LAST_DEPLOYED
                    && artifactSelection.getWorkflowId().equals(workflowId)))
        .collect(toList());
  }

  public List<Trigger> getTriggersHasArtifactStreamAction(String artifactStreamId) {
    return getTriggersByArtifactStreamSelection(artifactStreamId)
        .stream()
        .filter(trigger
            -> trigger.getArtifactSelections().stream().anyMatch(
                artifactSelection -> artifactSelection.getType() == LAST_COLLECTED))
        .collect(toList());
  }

  public List<Trigger> getTriggersHasArtifactStreamAction(String appId, String artifactStreamId) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return getTriggersHasArtifactStreamAction(artifactStreamId);
    }

    return getTriggersByApp(appId)
        .stream()
        .filter(trigger
            -> trigger.getArtifactSelections().stream().anyMatch(artifactSelection
                -> artifactSelection.getType() == LAST_COLLECTED
                    && artifactStreamId.equals(artifactSelection.getArtifactStreamId())))
        .collect(toList());
  }

  public List<Trigger> getNewInstanceTiggers(String appId) {
    return getTriggersByApp(appId)
        .stream()
        .filter(trigger -> trigger.getCondition().getConditionType() == TriggerConditionType.NEW_INSTANCE)
        .collect(toList());
  }

  public List<Trigger> getNewArtifactTriggers(String artifactStreamId) {
    return wingsPersistence.createQuery(Trigger.class, excludeAuthority)
        .disableValidation()
        .filter(TriggerKeys.condition + "." + TriggerConditionKeys.conditionType, NEW_ARTIFACT)
        .filter(TriggerKeys.condition + "." + ArtifactTriggerConditionKeys.artifactStreamId, artifactStreamId)
        .asList();
  }

  public List<Trigger> getNewArtifactTriggers(String appId, String artifactStreamId) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return getNewArtifactTriggers(artifactStreamId);
    }

    return getTriggersByApp(appId)
        .stream()
        .filter(tr
            -> tr.getCondition().getConditionType() == NEW_ARTIFACT
                && ((ArtifactTriggerCondition) tr.getCondition()).getArtifactStreamId().equals(artifactStreamId))
        .collect(toList());
  }

  public Trigger getTrigger(String appId, String webHookToken) {
    Trigger trigger = getTriggersByApp(appId)
                          .stream()
                          .filter(tr
                              -> tr.getCondition().getConditionType() == WEBHOOK
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
    return getMatchedSourcePipelineTriggers(appId, sourcePipelineId).collect(toList());
  }

  private Stream<Trigger> getMatchedSourcePipelineTriggers(String appId, String sourcePipelineId) {
    return getTriggersByApp(appId).stream().filter(trigger
        -> trigger.getCondition().getConditionType() == PIPELINE_COMPLETION
            && ((PipelineTriggerCondition) trigger.getCondition()).getPipelineId().equals(sourcePipelineId));
  }

  public List<Trigger> getTriggersByWorkflow(String appId, String pipelineId) {
    return wingsPersistence.createQuery(Trigger.class)
        .filter(TriggerKeys.appId, appId)
        .filter(TriggerKeys.workflowId, pipelineId)
        .asList();
  }

  public List<String> checkTemplatedEntityReferenced(String appId, String envId) {
    List<String> referencedTriggers = new ArrayList<>();
    try (HIterator<Trigger> triggerHIterator =
             new HIterator<>(wingsPersistence.createQuery(Trigger.class).filter(TriggerKeys.appId, appId).fetch())) {
      if (triggerHIterator != null) {
        for (Trigger trigger : triggerHIterator) {
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
      CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
      Cron cron = parser.parse(PREFIX + cronExpression);
      return CronDescriptor.instance(I18nMessages.DEFAULT_LOCALE).describe(cron) + " UTC";
    } catch (Exception e) {
      throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "Invalid cron expression");
    }
  }

  public static void validateAndSetCronExpression(Trigger trigger) {
    if (trigger == null || trigger.getCondition().getConditionType() != SCHEDULED) {
      return;
    }
    ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
    try {
      if (isNotBlank(scheduledTriggerCondition.getCronExpression())) {
        CronScheduleBuilder.cronSchedule(ScheduledTriggerJob.PREFIX + scheduledTriggerCondition.getCronExpression());
        scheduledTriggerCondition.setCronDescription(getCronDescription(scheduledTriggerCondition.getCronExpression()));
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
          WebHookToken.builder().httpMethod("POST").webHookToken(CryptoUtils.secureRandAlphaNumString(40)).build();
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
    notNullCheck("Workflow was deleted", workflow, USER);
    notNullCheck("Orchestration workflow was deleted", workflow.getOrchestrationWorkflow(), USER);
  }

  public static void addParameter(List<String> parameters, List<Variable> variables, boolean includeEntityType) {
    if (isEmpty(variables)) {
      return;
    }

    List<Variable> filteredVariables = includeEntityType
        ? variables
        : variables.stream().filter(variable -> variable.getType() != VariableType.ENTITY).collect(toList());

    for (Variable userVariable : filteredVariables) {
      if (!parameters.contains(userVariable.getName())) {
        parameters.add(userVariable.getName());
      }
    }
  }

  public List<String> obtainCollectedArtifactServiceIds(ExecutionArgs executionArgs) {
    final List<Artifact> artifacts = executionArgs.getArtifacts();
    if (isEmpty(artifacts)) {
      return new ArrayList<>();
    }

    Set<String> artifactServiceIds = new HashSet<>();
    artifacts.forEach(artifact -> {
      List<String> serviceIds = artifactStreamServiceBindingService.listServiceIds(artifact.getArtifactStreamId());
      if (isNotEmpty(serviceIds)) {
        artifactServiceIds.addAll(serviceIds);
      }
    });
    return new ArrayList<>(artifactServiceIds);
  }

  public static Map<String, String> overrideTriggerVariables(
      boolean infraDefEnabled, Trigger trigger, ExecutionArgs executionArgs, List<Variable> updatedVariables) {
    // Workflow variables come from Webhook
    Map<String, String> webhookVariableValues =
        executionArgs.getWorkflowVariables() == null ? new HashMap<>() : executionArgs.getWorkflowVariables();

    // Workflow variables associated with the trigger
    Map<String, String> triggerWorkflowVariableValues =
        trigger.getWorkflowVariables() == null ? new HashMap<>() : trigger.getWorkflowVariables();
    for (Entry<String, String> entry : webhookVariableValues.entrySet()) {
      if (isNotEmpty(entry.getValue())) {
        if (infraDefEnabled && entry.getKey().startsWith("ServiceInfra")) {
          String infraMappingVarName = entry.getKey();
          String infraDefVarName = infraMappingVarName.replace("ServiceInfra", "InfraDefinition");
          triggerWorkflowVariableValues.put(infraDefVarName, entry.getValue());
        } else {
          triggerWorkflowVariableValues.put(entry.getKey(), entry.getValue());
        }
      }
    }

    // Current updated variables present in pipeline/workflow
    List<String> updatedVariablesNames = updatedVariables != null
        ? updatedVariables.stream().map(Variable::getName).collect(toList())
        : new ArrayList<>();
    if (isNotEmpty(triggerWorkflowVariableValues)) {
      triggerWorkflowVariableValues.entrySet().removeIf(
          entry -> !updatedVariablesNames.contains(entry.getKey()) || isEmpty(entry.getValue()));
    }
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
      logger.warn("Invalid Build/Tag Filter {} for triggerId {}", artifactFilter, triggerId, pe);
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
