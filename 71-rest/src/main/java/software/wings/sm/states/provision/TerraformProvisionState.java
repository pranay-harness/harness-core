package software.wings.sm.states.provision;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.TaskType.TERRAFORM_PROVISION_TASK;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.service.intfc.FileService.FileBucket.TERRAFORM_STATE;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.reinert.jjschema.Attributes;
import com.mongodb.client.gridfs.model.GridFSFile;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.TerraformExecutionData;
import software.wings.api.TerraformOutputInfoElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.GitConfig;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.PhaseStep;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Validator;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class TerraformProvisionState extends State {
  private static final Logger logger = LoggerFactory.getLogger(TerraformProvisionState.class);

  private static final String VARIABLES_KEY = "variables";
  private static final String ENCRYPTED_VARIABLES_KEY = "encrypted_variables";

  @Inject @Transient private transient AppService appService;
  @Inject @Transient private transient ActivityService activityService;
  @Inject @Transient private transient InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject @Transient private transient DelegateService delegateService;
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject @Transient private transient FileService fileService;
  @Inject @Transient private transient ServiceVariableService serviceVariableService;
  @Inject @Transient private transient EncryptionService encryptionService;

  @Inject @Transient private transient WingsPersistence wingsPersistence;

  @Attributes(title = "Provisioner") @Getter @Setter String provisionerId;

  @Attributes(title = "Variables") @Getter @Setter private List<NameValuePair> variables;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   * @param stateType the state type
   */
  public TerraformProvisionState(String name, String stateType) {
    super(name, stateType);
  }

  protected abstract String commandUnit();

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // nothing to do
  }

  protected static Map<String, Object> parseOutputs(String all) {
    Map<String, Object> outputs = new LinkedHashMap<>();
    if (isBlank(all)) {
      return outputs;
    }

    try {
      TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
      Map<String, Object> json = new ObjectMapper().readValue(IOUtils.toInputStream(all), typeRef);

      json.forEach((key, object) -> outputs.put(key, ((Map<String, Object>) object).get("value")));

    } catch (IOException exception) {
      logger.error("", exception);
    }

    return outputs;
  }

  @SuppressFBWarnings("WMI_WRONG_MAP_ITERATOR")
  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    String activityId = response.keySet().iterator().next();
    TerraformExecutionData terraformExecutionData = (TerraformExecutionData) response.get(activityId);
    terraformExecutionData.setActivityId(activityId);

    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);

    Map<String, Object> others = null;
    if (!(this instanceof DestroyTerraformProvisionState)) {
      final Collection<NameValuePair> variables =
          calculateVariables(getVariables(), terraformProvisioner.getVariables());
      others = ImmutableMap.<String, Object>builder()
                   .put(VARIABLES_KEY,
                       variables.stream()
                           .filter(item -> item.getValue() != null)
                           .filter(item -> item.getValueType() == null || "TEXT".equals(item.getValueType()))
                           .collect(toMap(item -> item.getName(), item -> item.getValue())))
                   .put(ENCRYPTED_VARIABLES_KEY,
                       variables.stream()
                           .filter(item -> item.getValue() != null)
                           .filter(item -> "ENCRYPTED_TEXT".equals(item.getValueType()))
                           .collect(toMap(item -> item.getName(), item -> item.getValue())))
                   .build();
    }

    TerraformOutputInfoElement outputInfoElement = context.getContextElement(ContextElementType.TERRAFORM_PROVISION);
    if (outputInfoElement == null) {
      outputInfoElement = TerraformOutputInfoElement.builder().build();
    }
    if (terraformExecutionData.getExecutionStatus() == SUCCESS && terraformExecutionData.getOutputs() != null) {
      fileService.updateParentEntityIdAndVersion(PhaseStep.class, terraformExecutionData.getEntityId(), null,
          terraformExecutionData.getStateFileId(), others, FileBucket.TERRAFORM_STATE);
      Map<String, Object> outputs = parseOutputs(terraformExecutionData.getOutputs());
      Map<String, Object> contextOutputs = new HashMap<>();
      for (Map.Entry<String, Object> entry : outputs.entrySet()) {
        if (!(entry.getValue() instanceof List || entry.getValue() instanceof Map)) {
          contextOutputs.put(entry.getKey(), entry.getValue());
        }
      }
      outputInfoElement.addOutPuts(contextOutputs);
      infrastructureProvisionerService.regenerateInfrastructureMappings(provisionerId, context, outputs);
    }

    updateActivityStatus(activityId, context.getAppId(), terraformExecutionData.getExecutionStatus());

    // subsequent execution
    return anExecutionResponse()
        .withStateExecutionData(terraformExecutionData)
        .addContextElement(outputInfoElement)
        .addNotifyElement(outputInfoElement)
        .withExecutionStatus(terraformExecutionData.getExecutionStatus())
        .withErrorMessage(terraformExecutionData.getErrorMessage())
        .build();
  }

  protected void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }

  private Map<String, String> getVariables(Stream<NameValuePair> variables, ExecutionContext context) {
    return variables.filter(entry -> entry.getValue() != null)
        .filter(entry -> "TEXT".equals(entry.getValueType()))
        .collect(toMap(NameValuePair::getName, entry -> context.renderExpression(entry.getValue())));
  }

  private Map<String, EncryptedDataDetail> getEncryptedVariables(
      Stream<NameValuePair> variables, ExecutionContext context) {
    String accountId = appService.getAccountIdByAppId(context.getAppId());
    return variables.filter(entry -> entry.getValue() != null)
        .filter(entry -> "ENCRYPTED_TEXT".equals(entry.getValueType()))
        .collect(toMap(NameValuePair::getName, entry -> {
          final EncryptedData encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                                                  .filter(EncryptedData.ACCOUNT_ID_KEY, accountId)
                                                  .filter(EncryptedData.ID_KEY, entry.getValue())
                                                  .get();

          if (encryptedData == null) {
            throw new InvalidRequestException(format("The encrypted variable %s was not found", entry.getName()));
          }

          EncryptionConfig encryptionConfig =
              secretManager.getEncryptionConfig(accountId, encryptedData.getKmsId(), encryptedData.getEncryptionType());

          return EncryptedDataDetail.builder()
              .encryptionType(encryptedData.getEncryptionType())
              .encryptedData(encryptedData)
              .encryptionConfig(encryptionConfig)
              .build();
        }));
  }

  protected static Collection<NameValuePair> calculateVariables(
      List<NameValuePair> variables, List<NameValuePair> provisionerVariables) {
    Map<String, NameValuePair> map = new HashMap<>();
    if (isNotEmpty(provisionerVariables)) {
      provisionerVariables.forEach(variable -> map.put(variable.getName(), variable));
    }

    List<NameValuePair> list = new ArrayList<>();
    if (isNotEmpty(variables)) {
      variables.stream()
          .filter(variable -> {
            final NameValuePair nameValuePair = map.get(variable.getName());
            if (nameValuePair == null) {
              return false;
            }
            if (!Objects.equals(nameValuePair.getValueType(), variable.getValueType())) {
              throw new InvalidRequestException(format(
                  "The type of variable %s has changed. Please correct it in the workflow step.", variable.getName()));
            }
            return true;
          })
          .forEach(variable -> list.add(variable));
    }

    if (map.size() > list.size()) {
      throw new InvalidRequestException(
          "The provisioner requires more variables. Please correct it in the workflow step.");
    }

    return list;
  }

  private ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);

    SettingAttribute gitSettingAttribute = settingsService.get(terraformProvisioner.getSourceRepoSettingId());
    Validator.notNullCheck("gitSettingAttribute", gitSettingAttribute);
    if (!(gitSettingAttribute.getValue() instanceof GitConfig)) {
      throw new InvalidRequestException("");
    }

    GitConfig gitConfig = (GitConfig) gitSettingAttribute.getValue();

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;

    final String entityId = terraformProvisioner.getUuid() + "-" + executionContext.getEnv().getUuid();

    final String fileId = fileService.getLatestFileId(entityId, TERRAFORM_STATE);

    Map<String, String> variables = null;
    Map<String, EncryptedDataDetail> encryptedVariables = null;

    if (this instanceof DestroyTerraformProvisionState && fileId != null) {
      final GridFSFile gridFsFile = fileService.getGridFsFile(fileId, FileBucket.TERRAFORM_STATE);

      final Map<String, Object> rawVariables = (Map<String, Object>) gridFsFile.getExtraElements().get(VARIABLES_KEY);
      variables = getVariables(rawVariables.entrySet().stream().map(entry
                                   -> NameValuePair.builder()
                                          .valueType("TEXT")
                                          .name(entry.getKey())
                                          .value((String) entry.getValue())
                                          .build()),
          context);

      final Map<String, Object> rawEncryptedVariables =
          (Map<String, Object>) gridFsFile.getExtraElements().get(ENCRYPTED_VARIABLES_KEY);
      encryptedVariables = getEncryptedVariables(rawEncryptedVariables.entrySet().stream().map(entry
                                                     -> NameValuePair.builder()
                                                            .valueType("ENCRYPTED_TEXT")
                                                            .name(entry.getKey())
                                                            .value((String) entry.getValue())
                                                            .build()),
          context);
    } else {
      final Collection<NameValuePair> allVariables =
          calculateVariables(getVariables(), terraformProvisioner.getVariables());
      variables = getVariables(allVariables.stream(), context);
      encryptedVariables = getEncryptedVariables(allVariables.stream(), context);
    }

    DelegateTask delegateTask =
        aDelegateTask()
            .withTaskType(TERRAFORM_PROVISION_TASK)
            .withAccountId(executionContext.getApp().getAccountId())
            .withWaitId(activityId)
            .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
            .withParameters(new Object[] {
                TerraformProvisionParameters.builder()
                    .accountId(executionContext.getApp().getAccountId())
                    .appId(executionContext.getAppId())
                    .entityId(entityId)
                    .commandUnitName(commandUnit())
                    .currentStateFileId(fileId)
                    .activityId(activityId)
                    .sourceRepo(gitConfig)
                    .sourceRepoEncryptionDetails(secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null))
                    .scriptPath(terraformProvisioner.getPath())
                    .variables(variables)
                    .encryptedVariables(encryptedVariables)
                    .build()})
            .build();

    if (getTimeoutMillis() != null) {
      delegateTask.setTimeout(getTimeoutMillis());
    }
    String delegateTaskId = delegateService.queueTask(delegateTask);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .withStateExecutionData(ScriptStateExecutionData.builder().activityId(activityId).build())
        .build();
  }

  private TerraformInfrastructureProvisioner getTerraformInfrastructureProvisioner(ExecutionContext context) {
    final InfrastructureProvisioner infrastructureProvisioner =
        infrastructureProvisionerService.get(context.getAppId(), provisionerId);

    if (infrastructureProvisioner == null
        || (!(infrastructureProvisioner instanceof TerraformInfrastructureProvisioner))) {
      throw new InvalidRequestException("");
    }

    return (TerraformInfrastructureProvisioner) infrastructureProvisioner;
  }

  private String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    ActivityBuilder activityBuilder =
        Activity.builder()
            .applicationName(app.getName())
            .commandName(getName())
            .type(Activity.Type.Verification)
            .workflowType(executionContext.getWorkflowType())
            .workflowExecutionName(executionContext.getWorkflowExecutionName())
            .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
            .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
            .commandType(getStateType())
            .workflowExecutionId(executionContext.getWorkflowExecutionId())
            .workflowId(executionContext.getWorkflowId())
            .commandUnits(
                asList(Command.Builder.aCommand().withName(commandUnit()).withCommandType(CommandType.OTHER).build()))
            .status(ExecutionStatus.RUNNING);

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType().equals(BUILD)) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }

    Activity activity = activityBuilder.build();
    activity.setAppId(app.getUuid());
    return activityService.save(activity).getUuid();
  }
}
