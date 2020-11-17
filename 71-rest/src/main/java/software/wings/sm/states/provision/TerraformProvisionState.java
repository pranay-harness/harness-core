package software.wings.sm.states.provision;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.context.ContextElementType.TERRAFORM_INHERIT_PLAN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.service.DelegateAgentFileService.FileBucket.TERRAFORM_STATE;
import static io.harness.exception.WingsException.USER;
import static io.harness.provision.TerraformConstants.BACKEND_CONFIGS_KEY;
import static io.harness.provision.TerraformConstants.ENCRYPTED_BACKEND_CONFIGS_KEY;
import static io.harness.provision.TerraformConstants.ENCRYPTED_ENVIRONMENT_VARS_KEY;
import static io.harness.provision.TerraformConstants.ENCRYPTED_VARIABLES_KEY;
import static io.harness.provision.TerraformConstants.ENVIRONMENT_VARS_KEY;
import static io.harness.provision.TerraformConstants.QUALIFIER_APPLY;
import static io.harness.provision.TerraformConstants.TARGETS_KEY;
import static io.harness.provision.TerraformConstants.TF_APPLY_VAR_NAME;
import static io.harness.provision.TerraformConstants.TF_DESTROY_NAME_PREFIX;
import static io.harness.provision.TerraformConstants.TF_DESTROY_VAR_NAME;
import static io.harness.provision.TerraformConstants.TF_NAME_PREFIX;
import static io.harness.provision.TerraformConstants.TF_VAR_FILES_KEY;
import static io.harness.provision.TerraformConstants.VARIABLES_KEY;
import static io.harness.provision.TerraformConstants.WORKSPACE_KEY;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
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
import static software.wings.beans.TaskType.TERRAFORM_PROVISION_TASK;
import static software.wings.beans.delegation.TerraformProvisionParameters.TIMEOUT_IN_MINUTES;
import static software.wings.utils.Utils.splitCommaSeparatedFilePath;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.FileMetadata;
import io.harness.beans.SecretFile;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.data.algorithm.HashGenerator;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.service.DelegateAgentFileService.FileBucket;
import io.harness.exception.InvalidRequestException;
import io.harness.provision.TfVarScriptRepositorySource;
import io.harness.provision.TfVarSource;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.ResponseData;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.mongodb.morphia.query.Query;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.TerraformApplyMarkerParam;
import software.wings.api.TerraformExecutionData;
import software.wings.api.TerraformOutputInfoElement;
import software.wings.api.TerraformPlanParam;
import software.wings.api.terraform.TerraformProvisionInheritPlanElement;
import software.wings.api.terraform.TfVarGitSource;
import software.wings.app.MainConfiguration;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
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
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionContext;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class TerraformProvisionState extends State {
  @Inject private transient AppService appService;
  @Inject private transient ActivityService activityService;
  @Inject protected transient InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient GitUtilsManager gitUtilsManager;
  @Inject private transient GitFileConfigHelperService gitFileConfigHelperService;

  @Inject private transient ServiceVariableService serviceVariableService;
  @Inject private transient EncryptionService encryptionService;

  @Inject protected transient WingsPersistence wingsPersistence;
  @Inject protected transient DelegateService delegateService;
  @Inject protected transient FileService fileService;
  @Inject protected transient SecretManager secretManager;
  @Inject protected transient GitConfigHelperService gitConfigHelperService;
  @Inject private transient LogService logService;
  @Inject protected FeatureFlagService featureFlagService;
  @Inject protected SweepingOutputService sweepingOutputService;

  @Inject protected transient MainConfiguration configuration;

  @Attributes(title = "Provisioner") @Getter @Setter String provisionerId;

  @Attributes(title = "Variables") @Getter @Setter private List<NameValuePair> variables;
  @Attributes(title = "Backend Configs") @Getter @Setter private List<NameValuePair> backendConfigs;
  @Getter @Setter private List<NameValuePair> environmentVariables;
  @Getter @Setter private List<String> targets;

  @Getter @Setter private List<String> tfVarFiles;
  @Getter @Setter private GitFileConfig tfVarGitFileConfig;

  @Getter @Setter private boolean runPlanOnly;
  @Getter @Setter private boolean inheritApprovedPlan;
  @Getter @Setter private boolean exportPlanToApplyStep;
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
      log.error("", exception);
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

    if (exportPlanToApplyStep || (runPlanOnly && TerraformCommand.DESTROY == command())) {
      saveTerraformPlanToSecretManager(terraformExecutionData.getTfPlanFile(), context);
      fileService.updateParentEntityIdAndVersion(PhaseStep.class, terraformExecutionData.getEntityId(), null,
          terraformExecutionData.getStateFileId(), null, FileBucket.TERRAFORM_STATE);
    }
    saveTerraformPlanJson(terraformExecutionData.getTfPlanJson(), context, command());

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
            .environmentVariables(terraformExecutionData.getEnvironmentVariables())
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

  private void saveTerraformPlanJson(
      String terraformPlan, ExecutionContext context, TerraformCommand terraformCommand) {
    if (featureFlagService.isEnabled(FeatureName.EXPORT_TF_PLAN, context.getAccountId())) {
      String variableName = terraformCommand == TerraformCommand.APPLY ? TF_APPLY_VAR_NAME : TF_DESTROY_VAR_NAME;
      // if the plan variable exists overwrite it
      SweepingOutputInstance sweepingOutputInstance =
          sweepingOutputService.find(context.prepareSweepingOutputInquiryBuilder().name(variableName).build());
      if (sweepingOutputInstance != null) {
        sweepingOutputService.deleteById(context.getAppId(), sweepingOutputInstance.getUuid());
      }

      sweepingOutputService.save(context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.PIPELINE)
                                     .name(variableName)
                                     .value(TerraformPlanParam.builder()
                                                .tfplan(format("'%s'", JsonUtils.prettifyJsonString(terraformPlan)))
                                                .build())
                                     .build());
    }
  }

  private void saveTerraformPlanToSecretManager(byte[] terraformPlan, ExecutionContext context) {
    // if the plan exists in the secret manager, we need to overwrite it
    byte[] oldTerraformPLan = getTerraformPlanFromSecretManager(context);
    if (oldTerraformPLan != null) {
      deleteTerraformPlanFromSecretManager(context);
    }

    String planName = getPlanName(context);

    SecretFile secretFile = SecretFile.builder()
                                .fileContent(terraformPlan)
                                .name(planName)
                                .hideFromListing(true)
                                .kmsId(null)
                                .scopedToAccount(true)
                                .usageRestrictions(null)
                                .runtimeParameters(new HashMap<>())
                                .build();
    String terraformPlanSecretManagerId = secretManager.saveSecretFile(context.getAccountId(), secretFile);

    sweepingOutputService.save(
        context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
            .name(planName)
            .value(TerraformPlanParam.builder().terraformPlanSecretManagerId(terraformPlanSecretManagerId).build())
            .build());
  }

  @VisibleForTesting
  byte[] getTerraformPlanFromSecretManager(ExecutionContext context) {
    String planName = getPlanName(context);
    SweepingOutputInstance sweepingOutputInstance =
        sweepingOutputService.find(context.prepareSweepingOutputInquiryBuilder().name(planName).build());

    // if the reference to the plan file exists, use that plan in the apply step
    if (sweepingOutputInstance != null) {
      String terraformPlanSecretManagerId =
          ((TerraformPlanParam) sweepingOutputInstance.getValue()).getTerraformPlanSecretManagerId();
      return secretManager.getFileContents(context.getAccountId(), terraformPlanSecretManagerId);
    }
    return null;
  }

  private String getPlanName(ExecutionContext context) {
    String planPrefix = TerraformCommand.DESTROY == command() ? TF_DESTROY_NAME_PREFIX : TF_NAME_PREFIX;
    return String.format(planPrefix, context.getWorkflowExecutionId());
  }

  @VisibleForTesting
  void deleteTerraformPlanFromSecretManager(ExecutionContext context) {
    String planName = getPlanName(context);
    SweepingOutputInstance sweepingOutputInstance =
        sweepingOutputService.find(context.prepareSweepingOutputInquiryBuilder().name(planName).build());

    if (sweepingOutputInstance == null) {
      // we didn't find the plan reference in the sweeping output, there's nothing to delete
      return;
    }

    String terraformPlanSecretManagerId =
        ((TerraformPlanParam) sweepingOutputInstance.getValue()).getTerraformPlanSecretManagerId();
    secretManager.deleteSecret(context.getAccountId(), terraformPlanSecretManagerId, new HashMap<>(), false);

    // delete the plan reference from sweeping output
    sweepingOutputService.deleteById(context.getAppId(), sweepingOutputInstance.getUuid());
  }

  protected String getMarkerName() {
    return format("tfApplyCompleted_%s", provisionerId).trim();
  }

  private void markApplyExecutionCompleted(ExecutionContext context) {
    String markerName = getMarkerName();
    SweepingOutputInstance sweepingOutputInstance =
        sweepingOutputService.find(context.prepareSweepingOutputInquiryBuilder().name(markerName).build());
    if (sweepingOutputInstance != null) {
      return;
    }
    sweepingOutputInstance =
        context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
            .name(markerName)
            .value(TerraformApplyMarkerParam.builder().applyCompleted(true).provisionerId(provisionerId).build())
            .build();
    sweepingOutputService.save(sweepingOutputInstance);
  }

  private ExecutionResponse handleAsyncResponseInternalRegular(
      ExecutionContext context, Map<String, ResponseData> response) {
    Entry<String, ResponseData> responseEntry = response.entrySet().iterator().next();
    String activityId = responseEntry.getKey();
    TerraformExecutionData terraformExecutionData = (TerraformExecutionData) responseEntry.getValue();
    terraformExecutionData.setActivityId(activityId);

    // delete the plan if it exists
    deleteTerraformPlanFromSecretManager(context);

    TerraformInfrastructureProvisioner terraformProvisioner = getTerraformInfrastructureProvisioner(context);
    if (!(this instanceof DestroyTerraformProvisionState)) {
      markApplyExecutionCompleted(context);
    }

    if (terraformExecutionData.getExecutionStatus() == FAILED) {
      return ExecutionResponse.builder()
          .stateExecutionData(terraformExecutionData)
          .executionStatus(terraformExecutionData.getExecutionStatus())
          .errorMessage(terraformExecutionData.getErrorMessage())
          .build();
    }

    saveUserInputs(context, terraformExecutionData, terraformProvisioner);
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

  private void saveUserInputs(ExecutionContext context, TerraformExecutionData terraformExecutionData,
      TerraformInfrastructureProvisioner terraformProvisioner) {
    String workspace = terraformExecutionData.getWorkspace();
    Map<String, Object> others = new HashMap<>();
    if (!(this instanceof DestroyTerraformProvisionState)) {
      others.put("qualifier", QUALIFIER_APPLY);
      collectVariables(others, terraformExecutionData.getVariables(), VARIABLES_KEY, ENCRYPTED_VARIABLES_KEY, true);
      collectVariables(others, terraformExecutionData.getBackendConfigs(), BACKEND_CONFIGS_KEY,
          ENCRYPTED_BACKEND_CONFIGS_KEY, false);
      collectVariables(others, terraformExecutionData.getEnvironmentVariables(), ENVIRONMENT_VARS_KEY,
          ENCRYPTED_ENVIRONMENT_VARS_KEY, false);

      List<String> tfVarFiles = terraformExecutionData.getTfVarFiles();
      List<String> targets = terraformExecutionData.getTargets();

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
      if (getStateType().equals(StateType.TERRAFORM_DESTROY.name())) {
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

  private void collectVariables(Map<String, Object> others, List<NameValuePair> nameValuePairList, String varsKey,
      String encyptedVarsKey, boolean valueTypeCanBeNull) {
    if (isNotEmpty(nameValuePairList)) {
      others.put(varsKey,
          nameValuePairList.stream()
              .filter(item -> item.getValue() != null)
              .filter(item -> (valueTypeCanBeNull && item.getValueType() == null) || "TEXT".equals(item.getValueType()))
              .collect(toMap(NameValuePair::getName, NameValuePair::getValue)));
      others.put(encyptedVarsKey,
          nameValuePairList.stream()
              .filter(item -> item.getValue() != null)
              .filter(item -> "ENCRYPTED_TEXT".equals(item.getValueType()))
              .collect(toMap(NameValuePair::getName, NameValuePair::getValue)));
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
        ? provisionerVariables.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValueType))
        : new HashMap<>();
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
      throw new InvalidRequestException(
          "No previous Terraform plan execution found. Unable to inherit configuration from Terraform Plan");
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
      textVariables = infrastructureProvisionerService.extractUnresolvedTextVariables(allVariables);
      encryptedTextVariables =
          infrastructureProvisionerService.extractEncryptedTextVariables(allVariables, context.getAppId());
    }

    List<NameValuePair> allEnvVars = element.getEnvironmentVariables();
    Map<String, String> envVars = null;
    Map<String, EncryptedDataDetail> encryptedEnvVars = null;
    if (isNotEmpty(allEnvVars)) {
      envVars = infrastructureProvisionerService.extractUnresolvedTextVariables(allEnvVars);
      encryptedEnvVars = infrastructureProvisionerService.extractEncryptedTextVariables(allEnvVars, context.getAppId());
    }

    List<String> targets = element.getTargets();
    targets = resolveTargets(targets, context);

    gitConfigHelperService.convertToRepoGitConfig(
        gitConfig, context.renderExpression(terraformProvisioner.getRepoName()));

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .accountId(executionContext.getApp().getAccountId())
            .timeoutInMillis(defaultIfNullTimeout(TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES)))
            .activityId(activityId)
            .appId(executionContext.getAppId())
            .currentStateFileId(fileId)
            .entityId(entityId)
            .rawVariables(allVariables)
            .command(command())
            .commandUnit(commandUnit())
            .sourceRepoSettingId(element.getSourceRepoSettingId())
            .sourceRepo(gitConfig)
            .sourceRepoEncryptionDetails(
                secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, context.getWorkflowExecutionId()))
            .scriptPath(path)
            .variables(textVariables)
            .encryptedVariables(encryptedTextVariables)
            .backendConfigs(backendConfigs)
            .encryptedBackendConfigs(encryptedBackendConfigs)
            .environmentVariables(envVars)
            .encryptedEnvironmentVariables(encryptedEnvVars)
            .targets(targets)
            .tfVarFiles(element.getTfVarFiles())
            .runPlanOnly(false)
            .exportPlanToApplyStep(false)
            .terraformPlan(getTerraformPlanFromSecretManager(context))
            .workspace(workspace)
            .delegateTag(element.getDelegateTag())
            .skipRefreshBeforeApplyingPlan(terraformProvisioner.isSkipRefreshBeforeApplyingPlan())
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
    int expressionFunctorToken = HashGenerator.generateIntegerHash();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(requireNonNull(executionContext.getApp()).getAccountId())
            .waitId(activityId)
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, requireNonNull(executionContext.getApp()).getAppId())
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD,
                executionContext.getEnv() != null ? executionContext.getEnv().getUuid() : null)
            .tags(getRenderedTaskTags(delegateTag, executionContext))
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TERRAFORM_PROVISION_TASK.name())
                      .parameters(new Object[] {parameters})
                      .timeout(defaultIfNullTimeout(TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES)))
                      .expressionFunctorToken(expressionFunctorToken)
                      .build())
            .build();

    ScriptStateExecutionData stateExecutionData = ScriptStateExecutionData.builder().activityId(activityId).build();
    StateExecutionContext stateExecutionContext = StateExecutionContext.builder()
                                                      .stateExecutionData(stateExecutionData)
                                                      .adoptDelegateDecryption(true)
                                                      .expressionFunctorToken(expressionFunctorToken)
                                                      .build();
    renderDelegateTask(executionContext, delegateTask, stateExecutionContext);

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
    if (isNotEmpty(terraformProvisioner.getCommitId())) {
      String commitId = context.renderExpression(terraformProvisioner.getCommitId());
      if (isNotEmpty(commitId)) {
        gitConfig.setReference(commitId);
      }
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
    String entityId = generateEntityId(context, workspace);
    String fileId = fileService.getLatestFileId(entityId, TERRAFORM_STATE);
    Map<String, String> variables = null;
    Map<String, EncryptedDataDetail> encryptedVariables = null;
    Map<String, String> backendConfigs = null;
    Map<String, EncryptedDataDetail> encryptedBackendConfigs = null;
    Map<String, String> environmentVars = null;
    Map<String, EncryptedDataDetail> encryptedEnvironmentVars = null;
    List<NameValuePair> rawVariablesList = new ArrayList<>();

    if (isNotEmpty(this.variables) || isNotEmpty(this.backendConfigs) || isNotEmpty(this.environmentVariables)) {
      List<NameValuePair> validVariables =
          validateAndFilterVariables(getAllVariables(), terraformProvisioner.getVariables());
      rawVariablesList.addAll(validVariables);

      variables = infrastructureProvisionerService.extractUnresolvedTextVariables(validVariables);
      encryptedVariables =
          infrastructureProvisionerService.extractEncryptedTextVariables(validVariables, context.getAppId());

      if (this.backendConfigs != null) {
        backendConfigs = infrastructureProvisionerService.extractTextVariables(this.backendConfigs, context);
        encryptedBackendConfigs =
            infrastructureProvisionerService.extractEncryptedTextVariables(this.backendConfigs, context.getAppId());
      }

      if (this.environmentVariables != null) {
        List<NameValuePair> validEnvironmentVariables =
            validateAndFilterVariables(this.environmentVariables, terraformProvisioner.getEnvironmentVariables());
        environmentVars = infrastructureProvisionerService.extractUnresolvedTextVariables(validEnvironmentVariables);
        encryptedEnvironmentVars = infrastructureProvisionerService.extractEncryptedTextVariables(
            validEnvironmentVariables, context.getAppId());
      }

    } else if (this instanceof DestroyTerraformProvisionState) {
      fileId = fileService.getLatestFileIdByQualifier(entityId, TERRAFORM_STATE, QUALIFIER_APPLY);
      if (fileId != null) {
        FileMetadata fileMetadata = fileService.getFileMetadata(fileId, FileBucket.TERRAFORM_STATE);

        if (fileMetadata != null && fileMetadata.getMetadata() != null) {
          variables = extractData(fileMetadata, VARIABLES_KEY);
          Map<String, Object> rawVariables = (Map<String, Object>) fileMetadata.getMetadata().get(VARIABLES_KEY);
          if (isNotEmpty(rawVariables)) {
            rawVariablesList.addAll(extractVariables(rawVariables, "TEXT"));
          }

          backendConfigs = extractBackendConfigs(context, fileMetadata);

          encryptedVariables = extractEncryptedData(context, fileMetadata, ENCRYPTED_VARIABLES_KEY);
          Map<String, Object> rawEncryptedVariables =
              (Map<String, Object>) fileMetadata.getMetadata().get(ENCRYPTED_VARIABLES_KEY);
          if (isNotEmpty(rawEncryptedVariables)) {
            rawVariablesList.addAll(extractVariables(rawEncryptedVariables, "ENCRYPTED_TEXT"));
          }

          encryptedBackendConfigs = extractEncryptedData(context, fileMetadata, ENCRYPTED_BACKEND_CONFIGS_KEY);

          environmentVars = extractData(fileMetadata, ENVIRONMENT_VARS_KEY);
          encryptedEnvironmentVars = extractEncryptedData(context, fileMetadata, ENCRYPTED_ENVIRONMENT_VARS_KEY);

          List<String> targets = (List<String>) fileMetadata.getMetadata().get(TARGETS_KEY);
          if (isNotEmpty(targets)) {
            setTargets(targets);
          }

          List<String> tfVarFiles = (List<String>) fileMetadata.getMetadata().get(TF_VAR_FILES_KEY);
          if (isNotEmpty(tfVarFiles)) {
            setTfVarFiles(tfVarFiles);
          }
        }
      }
    }

    TfVarSource tfVarSource = null;

    // Currently we allow only one tfVar source
    if (isNotEmpty(tfVarFiles)) {
      tfVarSource = fetchTfVarScriptRepositorySource(context);
    } else if (null != tfVarGitFileConfig) {
      tfVarSource = fetchTfVarGitSource(context);
    }

    targets = resolveTargets(targets, context);
    gitConfigHelperService.convertToRepoGitConfig(
        gitConfig, context.renderExpression(terraformProvisioner.getRepoName()));

    if (runPlanOnly && this instanceof DestroyTerraformProvisionState) {
      exportPlanToApplyStep = true;
    }

    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .accountId(executionContext.getApp().getAccountId())
            .activityId(activityId)
            .timeoutInMillis(defaultIfNullTimeout(TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES)))
            .appId(executionContext.getAppId())
            .currentStateFileId(fileId)
            .entityId(entityId)
            .command(command())
            .commandUnit(commandUnit())
            .sourceRepoSettingId(terraformProvisioner.getSourceRepoSettingId())
            .sourceRepo(gitConfig)
            .sourceRepoEncryptionDetails(
                secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, context.getWorkflowExecutionId()))
            .scriptPath(path)
            .variables(variables)
            .rawVariables(rawVariablesList)
            .encryptedVariables(encryptedVariables)
            .backendConfigs(backendConfigs)
            .encryptedBackendConfigs(encryptedBackendConfigs)
            .environmentVariables(environmentVars)
            .encryptedEnvironmentVariables(encryptedEnvironmentVars)
            .targets(targets)
            .runPlanOnly(runPlanOnly)
            .exportPlanToApplyStep(exportPlanToApplyStep)
            .saveTerraformJson(featureFlagService.isEnabled(FeatureName.EXPORT_TF_PLAN, context.getAccountId()))
            .terraformPlan(null)
            .tfVarFiles(getRenderedTfVarFiles(tfVarFiles, context))
            .workspace(workspace)
            .delegateTag(delegateTag)
            .tfVarSource(tfVarSource)
            .skipRefreshBeforeApplyingPlan(terraformProvisioner.isSkipRefreshBeforeApplyingPlan())
            .build();

    return createAndRunTask(activityId, executionContext, parameters, delegateTag);
  }

  @VisibleForTesting
  TfVarScriptRepositorySource fetchTfVarScriptRepositorySource(ExecutionContext context) {
    return TfVarScriptRepositorySource.builder().tfVarFilePaths(getRenderedTfVarFiles(tfVarFiles, context)).build();
  }

  @VisibleForTesting
  TfVarGitSource fetchTfVarGitSource(ExecutionContext context) {
    GitConfig tfVarGitConfig = gitUtilsManager.getGitConfig(tfVarGitFileConfig.getConnectorId());
    gitConfigHelperService.renderGitConfig(context, tfVarGitConfig);
    gitFileConfigHelperService.renderGitFileConfig(context, tfVarGitFileConfig);

    gitConfigHelperService.convertToRepoGitConfig(
        tfVarGitConfig, context.renderExpression(tfVarGitFileConfig.getRepoName()));
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(tfVarGitConfig, GLOBAL_APP_ID, context.getWorkflowExecutionId());

    String filePath = tfVarGitFileConfig.getFilePath();
    if (filePath != null) {
      tfVarGitFileConfig.setFilePath(null);
      List<String> multipleFiles = splitCommaSeparatedFilePath(filePath);
      tfVarGitFileConfig.setFilePathList(multipleFiles);
    }

    return TfVarGitSource.builder()
        .gitConfig(tfVarGitConfig)
        .encryptedDataDetails(encryptionDetails)
        .gitFileConfig(tfVarGitFileConfig)
        .build();
  }

  private Map<String, String> extractData(FileMetadata fileMetadata, String dataKey) {
    Map<String, Object> rawData = (Map<String, Object>) fileMetadata.getMetadata().get(dataKey);
    if (isNotEmpty(rawData)) {
      return infrastructureProvisionerService.extractUnresolvedTextVariables(extractVariables(rawData, "TEXT"));
    }
    return null;
  }

  private Map<String, EncryptedDataDetail> extractEncryptedData(
      ExecutionContext context, FileMetadata fileMetadata, String encryptedDataKey) {
    Map<String, Object> rawData = (Map<String, Object>) fileMetadata.getMetadata().get(encryptedDataKey);
    Map<String, EncryptedDataDetail> encryptedData = null;
    if (isNotEmpty(rawData)) {
      encryptedData = infrastructureProvisionerService.extractEncryptedTextVariables(
          extractVariables(rawData, "ENCRYPTED_TEXT"), context.getAppId());
    }
    return encryptedData;
  }

  private Map<String, String> extractBackendConfigs(ExecutionContext context, FileMetadata fileMetadata) {
    Map<String, Object> rawBackendConfigs = (Map<String, Object>) fileMetadata.getMetadata().get(BACKEND_CONFIGS_KEY);
    if (isNotEmpty(rawBackendConfigs)) {
      return infrastructureProvisionerService.extractTextVariables(
          extractVariables(rawBackendConfigs, "TEXT"), context);
    }
    return null;
  }

  private List<NameValuePair> extractVariables(Map<String, Object> variables, String valueType) {
    return variables.entrySet()
        .stream()
        .map(entry
            -> NameValuePair.builder()
                   .valueType(valueType)
                   .name(entry.getKey())
                   .value((String) entry.getValue())
                   .build())
        .collect(toList());
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
                                          .environmentVariables(executionData.getEnvironmentVariables())
                                          .tfVarFiles(executionData.getTfVarFiles())
                                          .workflowExecutionId(context.getWorkflowExecutionId())
                                          .targets(executionData.getTargets())
                                          .command(executionData.getCommandExecuted())
                                          .appId(context.getAppId())
                                          .accountId(context.getAccountId())
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
    InfrastructureProvisioner infrastructureProvisioner =
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
        && executionContext.getOrchestrationWorkflowType() == BUILD) {
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
