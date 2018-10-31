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
import static software.wings.service.intfc.FileService.FileBucket.TERRAFORM_STATE;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.reinert.jjschema.Attributes;
import com.mongodb.client.gridfs.model.GridFSFile;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.TerraformExecutionData;
import software.wings.api.TerraformOutputInfoElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.GitConfig;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.PhaseStep;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandType;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.beans.infrastructure.TerraformfConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.GitConfigHelperService;
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
import software.wings.sm.StateType;
import software.wings.utils.Validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TerraformProvisionState extends State {
  private static final Logger logger = LoggerFactory.getLogger(TerraformProvisionState.class);

  private static final String VARIABLES_KEY = "variables";
  private static final String BACKEND_CONFIGS_KEY = "backend_configs";
  private static final String ENCRYPTED_VARIABLES_KEY = "encrypted_variables";
  private static final String ENCRYPTED_BACKEND_CONFIGS_KEY = "encrypted_backend_configs";
  private static final int DEFAULT_TERRAFORM_ASYNC_CALL_TIMEOUT = 30 * 60 * 1000; // 10 minutes

  @Inject @Transient private transient AppService appService;
  @Inject @Transient private transient ActivityService activityService;
  @Inject @Transient private transient InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private transient ServiceVariableService serviceVariableService;
  @Inject @Transient private transient EncryptionService encryptionService;

  @Inject @Transient protected transient WingsPersistence wingsPersistence;
  @Inject @Transient protected transient DelegateService delegateService;
  @Inject @Transient protected transient FileService fileService;
  @Inject @Transient protected transient SecretManager secretManager;
  @Inject @Transient protected transient GitConfigHelperService gitConfigHelperService;

  @Attributes(title = "Provisioner") @Getter @Setter String provisionerId;

  @Attributes(title = "Variables") @Getter @Setter private List<NameValuePair> variables;
  @Attributes(title = "Backend Configs") @Getter @Setter private List<NameValuePair> backendConfigs;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   * @param stateType the state type
   */
  public TerraformProvisionState(String name, String stateType) {
    super(name, stateType);
  }

  protected abstract TerraformCommandUnit commandUnit();
  protected abstract TerraformCommand command();

  @Attributes(title = "Timeout (ms)")
  @Override
  public Integer getTimeoutMillis() {
    return DEFAULT_TERRAFORM_ASYNC_CALL_TIMEOUT;
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

  static Map<String, Object> parseOutputs(String all) {
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

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    Entry<String, ResponseData> responseEntry = response.entrySet().iterator().next();
    String activityId = responseEntry.getKey();
    TerraformExecutionData terraformExecutionData = (TerraformExecutionData) responseEntry.getValue();
    terraformExecutionData.setActivityId(activityId);

    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);

    Map<String, Object> others = null;
    if (!(this instanceof DestroyTerraformProvisionState)) {
      final Collection<NameValuePair> variables =
          validateAndFilterVariables(getVariables(), terraformProvisioner.getVariables());
      Collection<NameValuePair> backendConfigs = terraformExecutionData.getBackendConfigs();

      others = ImmutableMap.<String, Object>builder()
                   .put(VARIABLES_KEY,
                       variables.stream()
                           .filter(item -> item.getValue() != null)
                           .filter(item -> item.getValueType() == null || "TEXT".equals(item.getValueType()))
                           .collect(toMap(NameValuePair::getName, NameValuePair::getValue)))
                   .put(ENCRYPTED_VARIABLES_KEY,
                       variables.stream()
                           .filter(item -> item.getValue() != null)
                           .filter(item -> "ENCRYPTED_TEXT".equals(item.getValueType()))
                           .collect(toMap(NameValuePair::getName, NameValuePair::getValue)))
                   .put(BACKEND_CONFIGS_KEY,
                       backendConfigs.stream()
                           .filter(item -> item.getValue() != null)
                           .filter(item -> "TEXT".equals(item.getValueType()))
                           .collect(toMap(NameValuePair::getName, NameValuePair::getValue)))
                   .put(ENCRYPTED_BACKEND_CONFIGS_KEY,
                       backendConfigs.stream()
                           .filter(item -> item.getValue() != null)
                           .filter(item -> "ENCRYPTED_TEXT".equals(item.getValueType()))
                           .collect(toMap(NameValuePair::getName, NameValuePair::getValue)))
                   .build();

      if (terraformExecutionData.getExecutionStatus() == SUCCESS) {
        saveTerraformConfig(context, terraformProvisioner, terraformExecutionData);
      }

    } else {
      if (this.getStateType().equals(StateType.TERRAFORM_DESTROY.name())) {
        if (terraformExecutionData.getExecutionStatus() == SUCCESS) {
          deleteTerraformConfig(context);
        }
      }
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

  protected Map<String, String> extractTextVariables(Stream<NameValuePair> variables, ExecutionContext context) {
    return variables.filter(entry -> entry.getValue() != null)
        .filter(entry -> "TEXT".equals(entry.getValueType()))
        .collect(toMap(NameValuePair::getName, entry -> context.renderExpression(entry.getValue())));
  }

  protected Map<String, EncryptedDataDetail> extractEncryptedTextVariables(
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

  protected static List<NameValuePair> validateAndFilterVariables(
      List<NameValuePair> variables, List<NameValuePair> provisionerVariables) {
    Map<String, String> variableTypesMap = provisionerVariables.stream().collect(
        Collectors.toMap(variable -> variable.getName(), variable -> variable.getValueType()));
    List<NameValuePair> validVariables = new ArrayList<>();
    if (isNotEmpty(variables)) {
      variables.stream()
          .filter(variable -> {
            if (!variableTypesMap.containsKey(variable.getName())) {
              return false;
            }

            if (!Objects.equals(variableTypesMap.get(variable.getName()), variable.getValueType())) {
              throw new InvalidRequestException(format(
                  "The type of variable %s has changed. Please correct it in the workflow step.", variable.getName()));
            }
            return true;
          })
          .forEach(variable -> validVariables.add(variable));
    }

    if (provisionerVariables.size() > validVariables.size()) {
      throw new InvalidRequestException(
          "The provisioner requires more variables. Please correct it in the workflow step.");
    }

    return validVariables;
  }

  protected GitConfig getGitConfig(String sourceRepoSettingId) {
    SettingAttribute gitSettingAttribute = settingsService.get(sourceRepoSettingId);
    Validator.notNullCheck("gitSettingAttribute", gitSettingAttribute);
    if (!(gitSettingAttribute.getValue() instanceof GitConfig)) {
      throw new InvalidRequestException("");
    }

    GitConfig gitConfig = (GitConfig) gitSettingAttribute.getValue();
    gitConfigHelperService.setSshKeySettingAttributeIfNeeded(gitConfig);
    return gitConfig;
  }

  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);

    GitConfig gitConfig = getGitConfig(terraformProvisioner.getSourceRepoSettingId());

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;

    final String entityId = generateEntityId(context);
    final String fileId = fileService.getLatestFileId(entityId, TERRAFORM_STATE);

    Map<String, String> variables = null;
    Map<String, EncryptedDataDetail> encryptedVariables = null;
    Map<String, String> backendConfigs = null;
    Map<String, EncryptedDataDetail> encryptedBackendConfigs = null;

    if (this instanceof DestroyTerraformProvisionState && fileId != null) {
      final GridFSFile gridFsFile = fileService.getGridFsFile(fileId, FileBucket.TERRAFORM_STATE);

      final Map<String, Object> rawVariables = (Map<String, Object>) gridFsFile.getExtraElements().get(VARIABLES_KEY);
      variables = extractTextVariables(rawVariables.entrySet().stream().map(entry
                                           -> NameValuePair.builder()
                                                  .valueType("TEXT")
                                                  .name(entry.getKey())
                                                  .value((String) entry.getValue())
                                                  .build()),
          context);

      final Map<String, Object> rawBackendConfigs =
          (Map<String, Object>) gridFsFile.getExtraElements().get(BACKEND_CONFIGS_KEY);
      backendConfigs = extractTextVariables(rawBackendConfigs.entrySet().stream().map(entry
                                                -> NameValuePair.builder()
                                                       .valueType("TEXT")
                                                       .name(entry.getKey())
                                                       .value((String) entry.getValue())
                                                       .build()),
          context);

      final Map<String, Object> rawEncryptedVariables =
          (Map<String, Object>) gridFsFile.getExtraElements().get(ENCRYPTED_VARIABLES_KEY);
      encryptedVariables = extractEncryptedTextVariables(rawEncryptedVariables.entrySet().stream().map(entry
                                                             -> NameValuePair.builder()
                                                                    .valueType("ENCRYPTED_TEXT")
                                                                    .name(entry.getKey())
                                                                    .value((String) entry.getValue())
                                                                    .build()),
          context);

      final Map<String, Object> rawEncryptedBackendConfigs =
          (Map<String, Object>) gridFsFile.getExtraElements().get(ENCRYPTED_BACKEND_CONFIGS_KEY);
      encryptedBackendConfigs = extractEncryptedTextVariables(rawEncryptedBackendConfigs.entrySet().stream().map(entry
                                                                  -> NameValuePair.builder()
                                                                         .valueType("ENCRYPTED_TEXT")
                                                                         .name(entry.getKey())
                                                                         .value((String) entry.getValue())
                                                                         .build()),
          context);
    } else {
      final Collection<NameValuePair> validVariables =
          validateAndFilterVariables(getAllVariables(), terraformProvisioner.getVariables());

      variables = extractTextVariables(validVariables.stream(), context);
      encryptedVariables = extractEncryptedTextVariables(validVariables.stream(), context);

      if (this.backendConfigs != null) {
        backendConfigs = extractTextVariables(this.backendConfigs.stream(), context);
        encryptedBackendConfigs = extractEncryptedTextVariables(this.backendConfigs.stream(), context);
      }
    }

    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .accountId(executionContext.getApp().getAccountId())
            .activityId(activityId)
            .appId(executionContext.getAppId())
            .currentStateFileId(fileId)
            .entityId(entityId)
            .command(command())
            .commandUnit(commandUnit())
            .sourceRepo(gitConfig)
            .sourceRepoEncryptionDetails(secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null))
            .scriptPath(terraformProvisioner.getPath())
            .variables(variables)
            .encryptedVariables(encryptedVariables)
            .backendConfigs(backendConfigs)
            .encryptedBackendConfigs(encryptedBackendConfigs)
            .build();

    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TERRAFORM_PROVISION_TASK)
                                    .withAccountId(executionContext.getApp().getAccountId())
                                    .withWaitId(activityId)
                                    .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
                                    .withParameters(new Object[] {parameters})
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

  /**
   * getVariables() returns all variables including backend configs.
   * for just the variables, this method should be called.
   * @return
   */
  private List<NameValuePair> getAllVariables() {
    return variables;
  }

  protected void saveTerraformConfig(
      ExecutionContext context, TerraformInfrastructureProvisioner provisioner, TerraformExecutionData executionData) {
    TerraformfConfig terraformfConfig = TerraformfConfig.builder()
                                            .entityId(generateEntityId(context))
                                            .sourceRepoSettingId(provisioner.getSourceRepoSettingId())
                                            .sourceRepoReference(executionData.getSourceRepoReference())
                                            .variables(executionData.getVariables())
                                            .backendConfigs(executionData.getBackendConfigs())
                                            .workflowExecutionId(context.getWorkflowExecutionId())
                                            .build();

    wingsPersistence.save(terraformfConfig);
  }

  protected String generateEntityId(ExecutionContext context) {
    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    return provisionerId + "-" + executionContext.getEnv().getUuid();
  }

  protected void deleteTerraformConfig(ExecutionContext context) {
    Query<TerraformfConfig> query = wingsPersistence.createQuery(TerraformfConfig.class)
                                        .filter(TerraformfConfig.ENTITY_ID_KEY, generateEntityId(context));

    wingsPersistence.delete(query);
  }

  protected TerraformInfrastructureProvisioner getTerraformInfrastructureProvisioner(ExecutionContext context) {
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
            .type(Type.Verification) // TODO : Change this to Type.Other
            .workflowType(executionContext.getWorkflowType())
            .workflowExecutionName(executionContext.getWorkflowExecutionName())
            .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
            .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
            .commandType(getStateType())
            .workflowExecutionId(executionContext.getWorkflowExecutionId())
            .workflowId(executionContext.getWorkflowId())
            .commandUnits(
                asList(Builder.aCommand().withName(commandUnit().name()).withCommandType(CommandType.OTHER).build()))
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
