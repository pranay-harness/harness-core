package software.wings.sm.states.provision;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.context.ContextElementType.TERRAFORM_INHERIT_PLAN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.FeatureName.TF_USE_VAR_CL;
import static software.wings.beans.TaskType.TERRAFORM_PROVISION_TASK;
import static software.wings.service.intfc.FileService.FileBucket.TERRAFORM_STATE;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.mongodb.morphia.query.Query;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.TerraformExecutionData;
import software.wings.api.TerraformOutputInfoElement;
import software.wings.api.terraform.TerraformProvisionInheritPlanElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.FileMetadata;
import software.wings.beans.GitConfig;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.PhaseStep;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandType;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.beans.infrastructure.TerraformConfig;
import software.wings.beans.infrastructure.TerraformConfig.TerraformConfigKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.utils.GitUtilsManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public abstract class TerraformProvisionState extends State {
  public static final String RUN_PLAN_ONLY_KEY = "runPlanOnly";
  public static final String INHERIT_APPROVED_PLAN = "inheritApprovedPlan";

  private static final String VARIABLES_KEY = "variables";
  private static final String BACKEND_CONFIGS_KEY = "backend_configs";
  private static final String TARGETS_KEY = "targets";
  private static final String TF_VAR_FILES_KEY = "tf_var_files";
  private static final String WORKSPACE_KEY = "tf_workspace";
  private static final String ENCRYPTED_VARIABLES_KEY = "encrypted_variables";
  private static final String ENCRYPTED_BACKEND_CONFIGS_KEY = "encrypted_backend_configs";
  private static final int DEFAULT_TERRAFORM_ASYNC_CALL_TIMEOUT = 30 * 60 * 1000; // 10 minutes

  @Inject private transient AppService appService;
  @Inject private transient ActivityService activityService;
  @Inject protected transient InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient GitUtilsManager gitUtilsManager;

  @Inject private transient ServiceVariableService serviceVariableService;
  @Inject private transient EncryptionService encryptionService;

  @Inject protected transient WingsPersistence wingsPersistence;
  @Inject protected transient DelegateService delegateService;
  @Inject protected transient FileService fileService;
  @Inject protected transient SecretManager secretManager;
  @Inject private transient GitConfigHelperService gitConfigHelperService;
  @Inject private transient LogService logService;
  @Inject protected FeatureFlagService featureFlagService;

  @Attributes(title = "Provisioner") @Getter @Setter String provisionerId;

  @Attributes(title = "Variables") @Getter @Setter private List<NameValuePair> variables;
  @Attributes(title = "Backend Configs") @Getter @Setter private List<NameValuePair> backendConfigs;
  @Getter @Setter private List<String> targets;

  @Getter @Setter private List<String> tfVarFiles;

  @Getter @Setter private boolean runPlanOnly;
  @Getter @Setter private boolean inheritApprovedPlan;
  @Getter @Setter private String workspace;
  @Getter @Setter private String delegateTag;

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
    if (runPlanOnly) {
      return handleAsyncResponseInternalRunPlanOnly(context, response);
    } else {
      return handleAsyncResponseInternalRegular(context, response);
    }
  }

  private ExecutionResponse handleAsyncResponseInternalRunPlanOnly(
      ExecutionContext context, Map<String, ResponseData> response) {
    Entry<String, ResponseData> responseEntry = response.entrySet().iterator().next();
    String activityId = responseEntry.getKey();
    TerraformExecutionData terraformExecutionData = (TerraformExecutionData) responseEntry.getValue();
    terraformExecutionData.setActivityId(activityId);
    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);
    updateActivityStatus(activityId, context.getAppId(), terraformExecutionData.getExecutionStatus());

    TerraformProvisionInheritPlanElement inheritPlanElement =
        TerraformProvisionInheritPlanElement.builder()
            .entityId(generateEntityId(context, terraformExecutionData.getWorkspace()))
            .provisionerId(provisionerId)
            .targets(terraformExecutionData.getTargets())
            .delegateTag(terraformExecutionData.getDelegateTag())
            .tfVarFiles(terraformExecutionData.getTfVarFiles())
            .sourceRepoSettingId(terraformProvisioner.getSourceRepoSettingId())
            .sourceRepoReference(terraformExecutionData.getSourceRepoReference())
            .variables(terraformExecutionData.getVariables())
            .backendConfigs(terraformExecutionData.getBackendConfigs())
            .workspace(terraformExecutionData.getWorkspace())
            .build();

    return ExecutionResponse.builder()
        .stateExecutionData(terraformExecutionData)
        .contextElement(inheritPlanElement)
        .notifyElement(inheritPlanElement)
        .executionStatus(terraformExecutionData.getExecutionStatus())
        .errorMessage(terraformExecutionData.getErrorMessage())
        .build();
  }

  private ExecutionResponse handleAsyncResponseInternalRegular(
      ExecutionContext context, Map<String, ResponseData> response) {
    Entry<String, ResponseData> responseEntry = response.entrySet().iterator().next();
    String activityId = responseEntry.getKey();
    TerraformExecutionData terraformExecutionData = (TerraformExecutionData) responseEntry.getValue();
    terraformExecutionData.setActivityId(activityId);

    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);
    saveUserInputs(context, terraformExecutionData, terraformProvisioner);
    if (terraformExecutionData.getExecutionStatus() == FAILED) {
      return ExecutionResponse.builder()
          .stateExecutionData(terraformExecutionData)
          .executionStatus(terraformExecutionData.getExecutionStatus())
          .errorMessage(terraformExecutionData.getErrorMessage())
          .build();
    } else {
      TerraformOutputInfoElement outputInfoElement = context.getContextElement(ContextElementType.TERRAFORM_PROVISION);
      if (outputInfoElement == null) {
        outputInfoElement = TerraformOutputInfoElement.builder().build();
      }
      if (terraformExecutionData.getOutputs() != null) {
        Map<String, Object> outputs = parseOutputs(terraformExecutionData.getOutputs());
        outputInfoElement.addOutPuts(outputs);
        ManagerExecutionLogCallback executionLogCallback = infrastructureProvisionerService.getManagerExecutionCallback(
            terraformProvisioner.getAppId(), terraformExecutionData.getActivityId(), commandUnit().name());
        infrastructureProvisionerService.regenerateInfrastructureMappings(
            provisionerId, context, outputs, Optional.of(executionLogCallback), Optional.empty());
      }

      updateActivityStatus(activityId, context.getAppId(), terraformExecutionData.getExecutionStatus());

      // subsequent execution
      return ExecutionResponse.builder()
          .stateExecutionData(terraformExecutionData)
          .contextElement(outputInfoElement)
          .notifyElement(outputInfoElement)
          .executionStatus(terraformExecutionData.getExecutionStatus())
          .errorMessage(terraformExecutionData.getErrorMessage())
          .build();
    }
  }

  private void saveUserInputs(ExecutionContext context, TerraformExecutionData terraformExecutionData,
      TerraformInfrastructureProvisioner terraformProvisioner) {
    String workspace = terraformExecutionData.getWorkspace();
    Map<String, Object> others = Maps.newHashMap();
    if (!(this instanceof DestroyTerraformProvisionState)) {
      List<NameValuePair> variables = terraformExecutionData.getVariables();
      List<NameValuePair> backendConfigs = terraformExecutionData.getBackendConfigs();
      List<String> tfVarFiles = terraformExecutionData.getTfVarFiles();
      List<String> targets = terraformExecutionData.getTargets();

      if (isNotEmpty(variables)) {
        others.put(VARIABLES_KEY,
            variables.stream()
                .filter(item -> item.getValue() != null)
                .filter(item -> item.getValueType() == null || "TEXT".equals(item.getValueType()))
                .collect(toMap(NameValuePair::getName, NameValuePair::getValue)));
        others.put(ENCRYPTED_VARIABLES_KEY,
            variables.stream()
                .filter(item -> item.getValue() != null)
                .filter(item -> "ENCRYPTED_TEXT".equals(item.getValueType()))
                .collect(toMap(NameValuePair::getName, NameValuePair::getValue)));
      }

      if (isNotEmpty(backendConfigs)) {
        others.put(BACKEND_CONFIGS_KEY,
            backendConfigs.stream()
                .filter(item -> item.getValue() != null)
                .filter(item -> "TEXT".equals(item.getValueType()))
                .collect(toMap(NameValuePair::getName, NameValuePair::getValue)));
        others.put(ENCRYPTED_BACKEND_CONFIGS_KEY,
            backendConfigs.stream()
                .filter(item -> item.getValue() != null)
                .filter(item -> "ENCRYPTED_TEXT".equals(item.getValueType()))
                .collect(toMap(NameValuePair::getName, NameValuePair::getValue)));
      }

      if (isNotEmpty(targets)) {
        others.put(TARGETS_KEY, targets);
      }

      if (isNotEmpty(tfVarFiles)) {
        others.put(TF_VAR_FILES_KEY, tfVarFiles);
      }

      if (isNotEmpty(workspace)) {
        others.put(WORKSPACE_KEY, workspace);
      }

      if (terraformExecutionData.getExecutionStatus() == SUCCESS) {
        saveTerraformConfig(context, terraformProvisioner, terraformExecutionData);
      }

    } else {
      if (this.getStateType().equals(StateType.TERRAFORM_DESTROY.name())) {
        if (terraformExecutionData.getExecutionStatus() == SUCCESS) {
          if (isNotEmpty(getTargets())) {
            saveTerraformConfig(context, terraformProvisioner, terraformExecutionData);
          } else {
            deleteTerraformConfig(context, terraformExecutionData);
          }
        }
      }
    }

    fileService.updateParentEntityIdAndVersion(PhaseStep.class, terraformExecutionData.getEntityId(), null,
        terraformExecutionData.getStateFileId(), others, FileBucket.TERRAFORM_STATE);
    if (isNotEmpty(workspace)) {
      updateProvisionerWorkspaces(terraformProvisioner, workspace);
    }
  }

  protected void updateProvisionerWorkspaces(
      TerraformInfrastructureProvisioner terraformProvisioner, String workspace) {
    if (isNotEmpty(terraformProvisioner.getWorkspaces()) && terraformProvisioner.getWorkspaces().contains(workspace)) {
      return;
    }
    List<String> workspaces =
        isNotEmpty(terraformProvisioner.getWorkspaces()) ? terraformProvisioner.getWorkspaces() : new ArrayList<>();
    workspaces.add(workspace);
    terraformProvisioner.setWorkspaces(workspaces);
    infrastructureProvisionerService.update(terraformProvisioner);
  }

  protected void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }

  protected static List<NameValuePair> validateAndFilterVariables(
      List<NameValuePair> workflowVariables, List<NameValuePair> provisionerVariables) {
    Map<String, String> variableTypesMap = isNotEmpty(provisionerVariables)
        ? provisionerVariables.stream().collect(
              Collectors.toMap(variable -> variable.getName(), variable -> variable.getValueType()))
        : Maps.newHashMap();
    List<NameValuePair> validVariables = new ArrayList<>();
    if (isNotEmpty(workflowVariables)) {
      workflowVariables.stream()
          .distinct()
          .filter(variable -> variableTypesMap.containsKey(variable.getName()))
          .forEach(validVariables::add);
    }

    return validVariables;
  }

  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    if (inheritApprovedPlan) {
      return executeInternalInherited(context, activityId);
    } else {
      return executeInternalRegular(context, activityId);
    }
  }

  private ExecutionResponse executeInternalInherited(ExecutionContext context, String activityId) {
    List<TerraformProvisionInheritPlanElement> allPlanElements = context.getContextElementList(TERRAFORM_INHERIT_PLAN);
    if (isEmpty(allPlanElements)) {
      throw new InvalidRequestException("No Terraform provision command with dry run found");
    }
    Optional<TerraformProvisionInheritPlanElement> elementOptional =
        allPlanElements.stream().filter(element -> element.getProvisionerId().equals(provisionerId)).findFirst();
    if (!elementOptional.isPresent()) {
      throw new InvalidRequestException("No Terraform provision command found with current provisioner");
    }
    TerraformProvisionInheritPlanElement element = elementOptional.get();

    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);
    String path = context.renderExpression(terraformProvisioner.getNormalizedPath());
    if (path == null) {
      path = context.renderExpression(FilenameUtils.normalize(terraformProvisioner.getPath()));
      if (path == null) {
        throw new InvalidRequestException("Invalid Terraform script path", USER);
      }
    }

    String workspace = context.renderExpression(element.getWorkspace());
    String entityId = generateEntityId(context, workspace);
    String fileId = fileService.getLatestFileId(entityId, TERRAFORM_STATE);
    GitConfig gitConfig = gitUtilsManager.getGitConfig(element.getSourceRepoSettingId());
    if (isNotEmpty(element.getSourceRepoReference())) {
      gitConfig.setReference(element.getSourceRepoReference());
      String branch = context.renderExpression(terraformProvisioner.getSourceRepoBranch());
      if (isNotEmpty(branch)) {
        gitConfig.setBranch(branch);
      }
    } else {
      throw new InvalidRequestException("No commit id found in context inherit tf plan element.");
    }

    List<NameValuePair> allBackendConfigs = element.getBackendConfigs();
    Map<String, String> backendConfigs = null;
    Map<String, EncryptedDataDetail> encryptedBackendConfigs = null;
    if (isNotEmpty(allBackendConfigs)) {
      backendConfigs = infrastructureProvisionerService.extractTextVariables(allBackendConfigs, context);
      encryptedBackendConfigs =
          infrastructureProvisionerService.extractEncryptedTextVariables(allBackendConfigs, context.getAppId());
    }

    List<NameValuePair> allVariables = element.getVariables();
    Map<String, String> textVariables = null;
    Map<String, EncryptedDataDetail> encryptedTextVariables = null;
    if (isNotEmpty(allVariables)) {
      textVariables = infrastructureProvisionerService.extractTextVariables(allVariables, context);
      encryptedTextVariables =
          infrastructureProvisionerService.extractEncryptedTextVariables(allVariables, context.getAppId());
    }

    List<String> targets = element.getTargets();
    targets = resolveTargets(targets, context);

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .accountId(executionContext.getApp().getAccountId())
            .activityId(activityId)
            .appId(executionContext.getAppId())
            .currentStateFileId(fileId)
            .entityId(entityId)
            .command(command())
            .commandUnit(commandUnit())
            .sourceRepoSettingId(element.getSourceRepoSettingId())
            .sourceRepo(gitConfig)
            .sourceRepoEncryptionDetails(secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null))
            .scriptPath(path)
            .variables(textVariables)
            .encryptedVariables(encryptedTextVariables)
            .backendConfigs(backendConfigs)
            .encryptedBackendConfigs(encryptedBackendConfigs)
            .targets(targets)
            .tfVarFiles(element.getTfVarFiles())
            .runPlanOnly(false)
            .workspace(workspace)
            .delegateTag(element.getDelegateTag())
            .useVarForInlineVariables(
                featureFlagService.isEnabled(TF_USE_VAR_CL, executionContext.getApp().getAccountId()))
            .build();

    return createAndRunTask(activityId, executionContext, parameters, element.getDelegateTag());
  }

  private List<String> getRenderedTaskTags(String rawTag, ExecutionContextImpl executionContext) {
    if (isEmpty(rawTag)) {
      return null;
    }
    return singletonList(executionContext.renderExpression(rawTag));
  }

  protected ExecutionResponse createAndRunTask(String activityId, ExecutionContextImpl executionContext,
      TerraformProvisionParameters parameters, String delegateTag) {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .async(true)
            .accountId(requireNonNull(executionContext.getApp()).getAccountId())
            .waitId(activityId)
            .appId(requireNonNull(executionContext.getApp()).getAppId())
            .envId(executionContext.getEnv() != null ? executionContext.getEnv().getUuid() : null)
            .tags(getRenderedTaskTags(delegateTag, executionContext))
            .data(TaskData.builder()
                      .taskType(TERRAFORM_PROVISION_TASK.name())
                      .parameters(new Object[] {parameters})
                      .timeout(defaultIfNullTimeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                      .build())
            .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(singletonList(activityId))
        .delegateTaskId(delegateTaskId)
        .stateExecutionData(ScriptStateExecutionData.builder().activityId(activityId).build())
        .build();
  }

  private ExecutionResponse executeInternalRegular(ExecutionContext context, String activityId) {
    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);
    GitConfig gitConfig = gitUtilsManager.getGitConfig(terraformProvisioner.getSourceRepoSettingId());
    String branch = context.renderExpression(terraformProvisioner.getSourceRepoBranch());
    if (isNotEmpty(branch)) {
      gitConfig.setBranch(branch);
    }
    String path = context.renderExpression(terraformProvisioner.getNormalizedPath());
    if (path == null) {
      path = context.renderExpression(FilenameUtils.normalize(terraformProvisioner.getPath()));
      if (path == null) {
        throw new InvalidRequestException("Invalid Terraform script path", USER);
      }
    }

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    String workspace = context.renderExpression(this.workspace);
    workspace = handleDefaultWorkspace(workspace);
    final String entityId = generateEntityId(context, workspace);
    final String fileId = fileService.getLatestFileId(entityId, TERRAFORM_STATE);

    Map<String, String> variables = null;
    Map<String, EncryptedDataDetail> encryptedVariables = null;
    Map<String, String> backendConfigs = null;
    Map<String, EncryptedDataDetail> encryptedBackendConfigs = null;

    if (this instanceof DestroyTerraformProvisionState && fileId != null) {
      final FileMetadata fileMetadata = fileService.getFileMetadata(fileId, FileBucket.TERRAFORM_STATE);

      if (fileMetadata != null && fileMetadata.getMetadata() != null) {
        final Map<String, Object> rawVariables = (Map<String, Object>) fileMetadata.getMetadata().get(VARIABLES_KEY);
        if (isNotEmpty(rawVariables)) {
          variables =
              infrastructureProvisionerService.extractTextVariables(rawVariables.entrySet()
                                                                        .stream()
                                                                        .map(entry
                                                                            -> NameValuePair.builder()
                                                                                   .valueType("TEXT")
                                                                                   .name(entry.getKey())
                                                                                   .value((String) entry.getValue())
                                                                                   .build())
                                                                        .collect(toList()),
                  context);
        }

        final Map<String, Object> rawBackendConfigs =
            (Map<String, Object>) fileMetadata.getMetadata().get(BACKEND_CONFIGS_KEY);
        if (isNotEmpty(rawBackendConfigs)) {
          backendConfigs =
              infrastructureProvisionerService.extractTextVariables(rawBackendConfigs.entrySet()
                                                                        .stream()
                                                                        .map(entry
                                                                            -> NameValuePair.builder()
                                                                                   .valueType("TEXT")
                                                                                   .name(entry.getKey())
                                                                                   .value((String) entry.getValue())
                                                                                   .build())
                                                                        .collect(toList()),
                  context);
        }

        final Map<String, Object> rawEncryptedVariables =
            (Map<String, Object>) fileMetadata.getMetadata().get(ENCRYPTED_VARIABLES_KEY);
        if (isNotEmpty(rawEncryptedVariables)) {
          encryptedVariables = infrastructureProvisionerService.extractEncryptedTextVariables(
              rawEncryptedVariables.entrySet()
                  .stream()
                  .map(entry
                      -> NameValuePair.builder()
                             .valueType("ENCRYPTED_TEXT")
                             .name(entry.getKey())
                             .value((String) entry.getValue())
                             .build())
                  .collect(toList()),
              context.getAppId());
        }

        final Map<String, Object> rawEncryptedBackendConfigs =
            (Map<String, Object>) fileMetadata.getMetadata().get(ENCRYPTED_BACKEND_CONFIGS_KEY);
        if (isNotEmpty(rawEncryptedBackendConfigs)) {
          encryptedBackendConfigs = infrastructureProvisionerService.extractEncryptedTextVariables(
              rawEncryptedBackendConfigs.entrySet()
                  .stream()
                  .map(entry
                      -> NameValuePair.builder()
                             .valueType("ENCRYPTED_TEXT")
                             .name(entry.getKey())
                             .value((String) entry.getValue())
                             .build())
                  .collect(toList()),
              context.getAppId());
        }
        List<String> targets = (List<String>) fileMetadata.getMetadata().get(TARGETS_KEY);
        if (isNotEmpty(targets)) {
          setTargets(targets);
        }

        List<String> tfVarFiles = (List<String>) fileMetadata.getMetadata().get(TF_VAR_FILES_KEY);
        if (isNotEmpty(tfVarFiles)) {
          setTfVarFiles(tfVarFiles);
        }
      }

    } else {
      logger.info("Variables before filtering: {}", getAllVariables());
      final List<NameValuePair> validVariables =
          validateAndFilterVariables(getAllVariables(), terraformProvisioner.getVariables());
      logger.info("Variables after filtering: {}", validVariables);

      variables = infrastructureProvisionerService.extractTextVariables(validVariables, context);
      logger.info("Variables after extracting text type: {}", variables);
      encryptedVariables =
          infrastructureProvisionerService.extractEncryptedTextVariables(validVariables, context.getAppId());

      if (this.backendConfigs != null) {
        backendConfigs = infrastructureProvisionerService.extractTextVariables(this.backendConfigs, context);
        encryptedBackendConfigs =
            infrastructureProvisionerService.extractEncryptedTextVariables(this.backendConfigs, context.getAppId());
      }
    }
    targets = resolveTargets(targets, context);

    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .accountId(executionContext.getApp().getAccountId())
            .activityId(activityId)
            .appId(executionContext.getAppId())
            .currentStateFileId(fileId)
            .entityId(entityId)
            .command(command())
            .commandUnit(commandUnit())
            .sourceRepoSettingId(terraformProvisioner.getSourceRepoSettingId())
            .sourceRepo(gitConfig)
            .sourceRepoEncryptionDetails(secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null))
            .scriptPath(path)
            .variables(variables)
            .encryptedVariables(encryptedVariables)
            .backendConfigs(backendConfigs)
            .encryptedBackendConfigs(encryptedBackendConfigs)
            .targets(targets)
            .runPlanOnly(runPlanOnly)
            .tfVarFiles(getRenderedTfVarFiles(tfVarFiles, context))
            .workspace(workspace)
            .delegateTag(delegateTag)
            .useVarForInlineVariables(
                featureFlagService.isEnabled(TF_USE_VAR_CL, executionContext.getApp().getAccountId()))
            .build();

    return createAndRunTask(activityId, executionContext, parameters, delegateTag);
  }

  protected String handleDefaultWorkspace(String workspace) {
    // Default is as good as no workspace
    return isNotEmpty(workspace) && workspace.equals("default") ? null : workspace;
  }

  private List<String> getRenderedTfVarFiles(List<String> tfVarFiles, ExecutionContext context) {
    if (isEmpty(tfVarFiles)) {
      return tfVarFiles;
    }
    return tfVarFiles.stream().map(context::renderExpression).collect(toList());
  }

  protected List<String> resolveTargets(List<String> targets, ExecutionContext context) {
    if (isEmpty(targets)) {
      return targets;
    }
    return targets.stream().map(context::renderExpression).collect(toList());
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
    TerraformConfig terraformConfig = TerraformConfig.builder()
                                          .entityId(generateEntityId(context, executionData.getWorkspace()))
                                          .sourceRepoSettingId(provisioner.getSourceRepoSettingId())
                                          .sourceRepoReference(executionData.getSourceRepoReference())
                                          .variables(executionData.getVariables())
                                          .delegateTag(executionData.getDelegateTag())
                                          .backendConfigs(executionData.getBackendConfigs())
                                          .tfVarFiles(executionData.getTfVarFiles())
                                          .workflowExecutionId(context.getWorkflowExecutionId())
                                          .targets(executionData.getTargets())
                                          .command(executionData.getCommandExecuted())
                                          .appId(context.getAppId())
                                          .build();
    wingsPersistence.save(terraformConfig);
  }

  protected String generateEntityId(ExecutionContext context, String workspace) {
    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    String envId = executionContext.getEnv() != null ? executionContext.getEnv().getUuid() : EMPTY;
    return isEmpty(workspace) ? (provisionerId + "-" + envId) : (provisionerId + "-" + envId + "-" + workspace);
  }

  protected void deleteTerraformConfig(ExecutionContext context, TerraformExecutionData terraformExecutionData) {
    Query<TerraformConfig> query =
        wingsPersistence.createQuery(TerraformConfig.class)
            .filter(TerraformConfigKeys.entityId, generateEntityId(context, terraformExecutionData.getWorkspace()));

    wingsPersistence.delete(query);
  }

  protected TerraformInfrastructureProvisioner getTerraformInfrastructureProvisioner(ExecutionContext context) {
    final InfrastructureProvisioner infrastructureProvisioner =
        infrastructureProvisionerService.get(context.getAppId(), provisionerId);

    if (infrastructureProvisioner == null) {
      throw new InvalidRequestException("Infrastructure Provisioner does not exist. Please check again.");
    }
    if (!(infrastructureProvisioner instanceof TerraformInfrastructureProvisioner)) {
      throw new InvalidRequestException("Infrastructure Provisioner " + infrastructureProvisioner.getName()
          + "should be of Terraform type. Please check again.");
    }
    return (TerraformInfrastructureProvisioner) infrastructureProvisioner;
  }

  private String createActivity(ExecutionContext executionContext) {
    Application app = requireNonNull(executionContext.getApp());
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

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
            .status(ExecutionStatus.RUNNING)
            .triggeredBy(TriggeredBy.builder()
                             .email(workflowStandardParams.getCurrentUser().getEmail())
                             .name(workflowStandardParams.getCurrentUser().getName())
                             .build());

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType().equals(BUILD)) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      Environment env = requireNonNull(((ExecutionContextImpl) executionContext).getEnv());
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }

    Activity activity = activityBuilder.build();
    activity.setAppId(app.getUuid());
    return activityService.save(activity).getUuid();
  }
}
