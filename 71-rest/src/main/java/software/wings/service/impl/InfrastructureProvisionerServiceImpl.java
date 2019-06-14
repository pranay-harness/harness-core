package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitEnforcementUtils;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.persistence.HIterator;
import io.harness.queue.Queue;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.validation.Create;
import io.harness.validation.Update;
import io.harness.waiter.ErrorNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.jexl3.JexlException;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.Key;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.BlueprintProperty;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.Event.Type;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerDetails;
import software.wings.beans.InfrastructureProvisionerDetails.InfrastructureProvisionerDetailsBuilder;
import software.wings.beans.Log;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.TerraformInputVariablesTaskResponse;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.prune.PruneEvent;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsCFHelperServiceManager;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.core.StreamingOutput;

@Singleton
@ValidateOnExecution
@Slf4j
public class InfrastructureProvisionerServiceImpl implements InfrastructureProvisionerService {
  @Inject private ManagerExpressionEvaluator evaluator;

  @Inject InfrastructureMappingService infrastructureMappingService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject SettingsService settingService;
  @Inject AwsCFHelperServiceManager awsCFHelperServiceManager;
  @Inject private AppService appService;
  @Inject YamlPushService yamlPushService;
  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;
  @Inject private GitConfigHelperService gitConfigHelperService;
  @Inject FileService fileService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private LogService logService;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private Queue<PruneEvent> pruneQueue;

  @Inject private LimitCheckerFactory limitCheckerFactory;

  @Override
  @ValidationGroups(Create.class)
  public InfrastructureProvisioner save(@Valid InfrastructureProvisioner infrastructureProvisioner) {
    String accountId = appService.getAccountIdByAppId(infrastructureProvisioner.getAppId());
    infrastructureProvisioner.setAccountId(accountId);
    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_INFRA_PROVISIONER));

    return LimitEnforcementUtils.withLimitCheck(checker, () -> {
      populateDerivedFields(infrastructureProvisioner);

      InfrastructureProvisioner finalInfraProvisioner =
          wingsPersistence.saveAndGet(InfrastructureProvisioner.class, infrastructureProvisioner);

      yamlPushService.pushYamlChangeSet(
          accountId, null, infrastructureProvisioner, Type.CREATE, infrastructureProvisioner.isSyncFromGit(), false);

      return finalInfraProvisioner;
    });
  }

  private void populateDerivedFields(InfrastructureProvisioner infrastructureProvisioner) {
    if (infrastructureProvisioner instanceof TerraformInfrastructureProvisioner) {
      TerraformInfrastructureProvisioner terraformInfrastructureProvisioner =
          (TerraformInfrastructureProvisioner) infrastructureProvisioner;
      terraformInfrastructureProvisioner.setTemplatized(isTemplatizedProvisioner(terraformInfrastructureProvisioner));
      terraformInfrastructureProvisioner.setNormalizedPath(
          FilenameUtils.normalize(terraformInfrastructureProvisioner.getPath()));
    }
  }

  @Override
  @ValidationGroups(Update.class)
  public InfrastructureProvisioner update(@Valid InfrastructureProvisioner infrastructureProvisioner) {
    populateDerivedFields(infrastructureProvisioner);
    InfrastructureProvisioner savedInfraProvisioner =
        get(infrastructureProvisioner.getAppId(), infrastructureProvisioner.getUuid());

    InfrastructureProvisioner updatedInfraProvisioner =
        wingsPersistence.saveAndGet(InfrastructureProvisioner.class, infrastructureProvisioner);

    String accountId = appService.getAccountIdByAppId(infrastructureProvisioner.getAppId());
    boolean isRename = !infrastructureProvisioner.getName().equals(savedInfraProvisioner.getName());
    yamlPushService.pushYamlChangeSet(accountId, savedInfraProvisioner, updatedInfraProvisioner, Type.UPDATE,
        infrastructureProvisioner.isSyncFromGit(), isRename);

    return updatedInfraProvisioner;
  }

  @Override
  public PageResponse<InfrastructureProvisioner> list(PageRequest<InfrastructureProvisioner> pageRequest) {
    return wingsPersistence.query(InfrastructureProvisioner.class, pageRequest);
  }

  @Override
  public PageResponse<InfrastructureProvisioner> listByBlueprintDetails(@NotEmpty String appId,
      String infrastructureProvisionerType, String serviceId, DeploymentType deploymentType,
      CloudProviderType cloudProviderType) {
    PageRequestBuilder requestBuilder = aPageRequest();

    String accountIdByAppId = appService.getAccountIdByAppId(appId);
    if (!featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountIdByAppId)) {
      if (serviceId != null) {
        requestBuilder.addFilter(InfrastructureMappingBlueprint.SERVICE_ID_KEY, Operator.EQ, serviceId);
      }
      if (deploymentType != null) {
        requestBuilder.addFilter(InfrastructureMappingBlueprint.DEPLOYMENT_TYPE_KEY, Operator.EQ, deploymentType);
      }
      if (cloudProviderType != null) {
        requestBuilder.addFilter(
            InfrastructureMappingBlueprint.CLOUD_PROVIDER_TYPE_KEY, Operator.EQ, cloudProviderType);
      }
    }

    final PageRequest blueprintRequest = requestBuilder.build();
    requestBuilder = aPageRequest().addFilter(InfrastructureProvisioner.APP_ID_KEY, Operator.EQ, appId);

    if (infrastructureProvisionerType != null) {
      requestBuilder.addFilter(
          InfrastructureProvisioner.INFRASTRUCTURE_PROVISIONER_TYPE_KEY, Operator.EQ, infrastructureProvisionerType);
    }

    if (!featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountIdByAppId)) {
      if (isNotEmpty(blueprintRequest.getFilters())) {
        requestBuilder.addFilter(
            InfrastructureProvisioner.MAPPING_BLUEPRINTS_KEY, Operator.ELEMENT_MATCH, blueprintRequest);
      }
    }

    return wingsPersistence.query(InfrastructureProvisioner.class, requestBuilder.build());
  }

  private InfrastructureProvisionerDetails details(InfrastructureProvisioner provisioner) {
    final InfrastructureProvisionerDetailsBuilder detailsBuilder =
        InfrastructureProvisionerDetails.builder()
            .uuid(provisioner.getUuid())
            .name(provisioner.getName())
            .description(provisioner.getDescription())
            .infrastructureProvisionerType(provisioner.getInfrastructureProvisionerType());

    if (provisioner instanceof TerraformInfrastructureProvisioner) {
      final TerraformInfrastructureProvisioner terraformInfrastructureProvisioner =
          (TerraformInfrastructureProvisioner) provisioner;

      final SettingAttribute settingAttribute =
          settingService.get(terraformInfrastructureProvisioner.getSourceRepoSettingId());

      if (settingAttribute != null && settingAttribute.getValue() instanceof GitConfig) {
        detailsBuilder.repository(((GitConfig) settingAttribute.getValue()).getRepoUrl());
      }
    } else if (provisioner instanceof CloudFormationInfrastructureProvisioner) {
      CloudFormationInfrastructureProvisioner cloudFormationInfrastructureProvisioner =
          (CloudFormationInfrastructureProvisioner) provisioner;
      String sourceType = "UNKNOWN";
      if (cloudFormationInfrastructureProvisioner.provisionByBody()) {
        sourceType = "Template Body";
      } else if (cloudFormationInfrastructureProvisioner.provisionByUrl()) {
        sourceType = "Amazon S3";
      }
      detailsBuilder.cloudFormationSourceType(sourceType);
    }

    if (isNotEmpty(provisioner.getMappingBlueprints())) {
      detailsBuilder.services(provisioner.getMappingBlueprints()
                                  .stream()
                                  .map(InfrastructureMappingBlueprint::getServiceId)
                                  .map(serviceId -> serviceResourceService.get(provisioner.getAppId(), serviceId))
                                  .collect(toMap(service -> service.getName(), service -> service.getUuid())));
    }

    return detailsBuilder.build();
  }

  @Override
  public PageResponse<InfrastructureProvisionerDetails> listDetails(
      PageRequest<InfrastructureProvisioner> pageRequest) {
    return aPageResponse()
        .withResponse(wingsPersistence.query(InfrastructureProvisioner.class, pageRequest)
                          .stream()
                          .map(item -> details(item))
                          .collect(toList()))
        .build();
  }

  @Override
  public InfrastructureProvisioner get(String appId, String infrastructureProvisionerId) {
    InfrastructureProvisioner infrastructureProvisioner =
        wingsPersistence.getWithAppId(InfrastructureProvisioner.class, appId, infrastructureProvisionerId);
    if (infrastructureProvisioner == null) {
      throw new InvalidRequestException("Provisioner Not Found");
    }
    return infrastructureProvisioner;
  }

  @Override
  public InfrastructureProvisioner getByName(String appId, String provisionerName) {
    return wingsPersistence.createQuery(InfrastructureProvisioner.class)
        .filter(InfrastructureProvisioner.APP_ID_KEY, appId)
        .filter(InfrastructureProvisioner.NAME_KEY, provisionerName)
        .get();
  }

  private void ensureSafeToDelete(String appId, String infrastructureProvisionerId) {
    List<Key<InfrastructureMapping>> keys =
        wingsPersistence.createQuery(InfrastructureMapping.class)
            .filter(InfrastructureMapping.APP_ID_KEY, appId)
            .filter(InfrastructureMapping.PROVISIONER_ID_KEY, infrastructureProvisionerId)
            .asKeyList();
    try {
      for (Key<InfrastructureMapping> key : keys) {
        infrastructureMappingService.ensureSafeToDelete(appId, (String) key.getId());
      }
    } catch (Exception exception) {
      throw new InvalidRequestException(
          format("Infrastructure provisioner %s is not safe to delete", infrastructureProvisionerId), exception, USER);
    }
  }

  @Override
  public void delete(String appId, String infrastructureProvisionerId) {
    delete(appId, infrastructureProvisionerId, false);
  }

  @Override
  public void delete(String appId, String infrastructureProvisionerId, boolean syncFromGit) {
    InfrastructureProvisioner infrastructureProvisioner = get(appId, infrastructureProvisionerId);

    ensureSafeToDelete(appId, infrastructureProvisionerId);

    String accountId = appService.getAccountIdByAppId(infrastructureProvisioner.getAppId());
    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_INFRA_PROVISIONER));

    LimitEnforcementUtils.withCounterDecrement(checker, () -> {
      ensureSafeToDelete(appId, infrastructureProvisionerId);

      yamlPushService.pushYamlChangeSet(accountId, infrastructureProvisioner, null, Type.DELETE, syncFromGit, false);

      wingsPersistence.delete(InfrastructureProvisioner.class, appId, infrastructureProvisionerId);
    });
  }

  @Override
  public void pruneDescendingEntities(String appId, String infrastructureProvisionerId) {}

  private void prune(String appId, String infraProvisionerId) {
    pruneQueue.send(new PruneEvent(InfrastructureProvisioner.class, appId, infraProvisionerId));
    delete(appId, infraProvisionerId);
  }

  @Override
  public void pruneByApplication(String appId) {
    List<Key<InfrastructureProvisioner>> keys = wingsPersistence.createQuery(InfrastructureProvisioner.class)
                                                    .filter(InfrastructureProvisioner.APP_ID_KEY, appId)
                                                    .asKeyList();
    for (Key<InfrastructureProvisioner> key : keys) {
      prune(appId, (String) key.getId());
    }
  }

  // Region is optional and is not present in the blue print properties for cloud formation provisioners and
  // present for terraform
  private void applyProperties(Map<String, Object> contextMap, InfrastructureMapping infrastructureMapping,
      List<BlueprintProperty> properties, Optional<ManagerExecutionLogCallback> executionLogCallbackOptional,
      Optional<String> region, NodeFilteringType nodeFilteringType) {
    Map<String, Object> propertyNameEvaluatedMap =
        generateMapToUpdateInfraMapping(contextMap, properties, executionLogCallbackOptional, region);

    try {
      infrastructureMapping.applyProvisionerVariables(propertyNameEvaluatedMap, nodeFilteringType);
      infrastructureMappingService.update(infrastructureMapping);
    } catch (Exception e) {
      addToExecutionLog(executionLogCallbackOptional, ExceptionUtils.getMessage(e));
      throw e;
    }
  }

  private Map<String, Object> generateMapToUpdateInfraMapping(Map<String, Object> contextMap,
      List<BlueprintProperty> properties, Optional<ManagerExecutionLogCallback> executionLogCallbackOptional,
      Optional<String> region) {
    Map<String, Object> propertyNameEvaluatedMap = getPropertyNameEvaluatedMap(
        properties.stream()
            .map(property -> new NameValuePair(property.getName(), property.getValue(), property.getValueType()))
            .collect(toList()),
        contextMap, executionLogCallbackOptional);

    for (BlueprintProperty property : properties) {
      if (isNotEmpty(property.getFields())) {
        String propertyName = property.getName();
        Object evaluatedValue = propertyNameEvaluatedMap.get(propertyName);
        if (evaluatedValue == null) {
          throw new InvalidRequestException(String.format("Property %s not found for resolving fields", propertyName));
        }
        List<NameValuePair> fields = property.getFields();
        if (evaluatedValue instanceof List) {
          List<Map<String, Object>> fieldsEvaluatedList = new ArrayList<>();
          for (Object evaluatedEntry : (List) evaluatedValue) {
            fieldsEvaluatedList.add(getPropertyNameEvaluatedMap(
                fields, (Map<String, Object>) evaluatedEntry, executionLogCallbackOptional));
          }
          propertyNameEvaluatedMap.put(propertyName, fieldsEvaluatedList);

        } else {
          getPropertyNameEvaluatedMap(fields, (Map<String, Object>) evaluatedValue, executionLogCallbackOptional);
        }
      }
    }

    region.ifPresent(s -> propertyNameEvaluatedMap.put("region", s));
    return propertyNameEvaluatedMap;
  }

  @NotNull
  private Map<String, Object> getPropertyNameEvaluatedMap(List<NameValuePair> properties,
      Map<String, Object> contextMap, Optional<ManagerExecutionLogCallback> executionLogCallbackOptional) {
    Map<String, Object> propertyNameEvaluatedMap = new HashMap<>();
    for (NameValuePair property : properties) {
      if (property.getValue() == null) {
        continue;
      }
      Object evaluated;
      try {
        evaluated = evaluator.evaluate(property.getValue(), contextMap);
      } catch (Exception exception) {
        String errorMsg =
            String.format("The infrastructure provisioner mapping %s was not resolved from the provisioner outputs",
                property.getName());
        addToExecutionLog(executionLogCallbackOptional, errorMsg);
        throw new InvalidRequestException(errorMsg, exception, USER);
      }
      if (evaluated == null) {
        String errorMsg =
            String.format("The infrastructure provisioner mapping %s was not resolved from the provisioner outputs",
                property.getName());
        addToExecutionLog(executionLogCallbackOptional, errorMsg);
        throw new InvalidRequestException(errorMsg);
      }
      propertyNameEvaluatedMap.put(property.getName(), evaluated);
    }
    return propertyNameEvaluatedMap;
  }

  private void addToExecutionLog(Optional<ManagerExecutionLogCallback> executionLogCallbackOptional, String msg) {
    if (executionLogCallbackOptional.isPresent()) {
      executionLogCallbackOptional.get().saveExecutionLog("# " + msg);
    }
  }

  @Override
  public void regenerateInfrastructureMappings(
      String provisionerId, ExecutionContext context, Map<String, Object> outputs) {
    regenerateInfrastructureMappings(provisionerId, context, outputs, Optional.empty(), Optional.empty());
  }

  @Override
  public void regenerateInfrastructureMappings(String provisionerId, ExecutionContext context,
      Map<String, Object> outputs, Optional<ManagerExecutionLogCallback> executionLogCallbackOptional,
      Optional<String> region) {
    final InfrastructureProvisioner infrastructureProvisioner = get(context.getAppId(), provisionerId);

    final Map<String, Object> contextMap = context.asMap();
    contextMap.put(infrastructureProvisioner.variableKey(), outputs);
    contextMap.putAll(outputs);

    try (HIterator<InfrastructureMapping> infrastructureMappings =
             new HIterator<>(wingsPersistence.createQuery(InfrastructureMapping.class)
                                 .filter(InfrastructureMapping.APP_ID_KEY, infrastructureProvisioner.getAppId())
                                 .filter(InfrastructureMapping.PROVISIONER_ID_KEY, infrastructureProvisioner.getUuid())
                                 .fetch())) {
      if (infrastructureMappings.hasNext()) {
        boolean featureFlagEnabled =
            featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, infrastructureProvisioner.getAccountId());
        while (infrastructureMappings.hasNext()) {
          InfrastructureMapping infrastructureMapping = infrastructureMappings.next();

          if (featureFlagEnabled) {
            Map<String, Object> resolvedBlueprints =
                resolveBlueprints(contextMap, infrastructureMapping.getBlueprints(), infrastructureMapping.getName());
            updateInfraMapping(infrastructureMapping, resolvedBlueprints);
            addToExecutionLog(
                executionLogCallbackOptional, "Updated service infra \"" + infrastructureMapping.getName() + "\"");
          } else {
            if (isEmpty(infrastructureProvisioner.getMappingBlueprints())) {
              throw new InvalidRequestException(
                  "Service Mapping not found for Service Infra : " + infrastructureMapping.getName()
                      + ". Add Service Mapping or de-link provisioner from Service Infra to resolve.",
                  USER);
            }

            infrastructureProvisioner.getMappingBlueprints()
                .stream()
                .filter(blueprint -> blueprint.getServiceId().equals(infrastructureMapping.getServiceId()))
                .filter(blueprint
                    -> blueprint.infrastructureMappingType().name().equals(infrastructureMapping.getInfraMappingType()))
                .forEach(blueprint -> {
                  logger.info("Provisioner {} updates infrastructureMapping {}", infrastructureProvisioner.getUuid(),
                      infrastructureMapping.getUuid());
                  addToExecutionLog(executionLogCallbackOptional,
                      "Updating service infra \"" + infrastructureMapping.getName() + "\"");
                  applyProperties(contextMap, infrastructureMapping, blueprint.getProperties(),
                      executionLogCallbackOptional, region, blueprint.getNodeFilteringType());
                });
          }
        }
      }
    }
  }

  private void updateInfraMapping(InfrastructureMapping infrastructureMapping, Map<String, Object> resolvedBlueprints) {
    infrastructureMapping.applyProvisionerVariables(resolvedBlueprints, null);
    infrastructureMappingService.update(infrastructureMapping);
  }

  @VisibleForTesting
  public Map<String, Object> resolveBlueprints(
      Map<String, Object> contextMap, Map<String, String> blueprints, String infraMappingName) {
    if (isEmpty(blueprints)) {
      throw new InvalidRequestException(
          "Invalid Configuration. Blueprints does not exists for service infra \"" + infraMappingName + "\"");
    }
    Map<String, Object> resolvedBlueprints = new HashMap<>(blueprints.size());

    for (Map.Entry<String, String> blueprint : blueprints.entrySet()) {
      Object evaluated;
      try {
        evaluated = evaluator.evaluate(blueprint.getValue(), contextMap);
      } catch (JexlException.Variable ex) {
        throw new InvalidRequestException(
            "Unable to resolve blueprint \"" + blueprint.getValue() + "\" from outputs", USER);
      }
      resolvedBlueprints.put(blueprint.getKey(), evaluated);
    }
    return resolvedBlueprints;
  }

  @Override
  public List<AwsCFTemplateParamsData> getCFTemplateParamKeys(
      String type, String region, String awsConfigId, String data, String appId) {
    return awsCFHelperServiceManager.getParamsData(type, data, awsConfigId, region, appId);
  }

  @Override
  public List<NameValuePair> getTerraformVariables(
      String appId, String scmSettingId, String terraformDirectory, String accountId, String sourceRepoBranch) {
    SettingAttribute gitSettingAttribute = settingService.get(scmSettingId);
    Validator.notNullCheck("Source repo provided is not Valid", gitSettingAttribute);
    if (!(gitSettingAttribute.getValue() instanceof GitConfig)) {
      throw new InvalidRequestException("Source repo provided is not Valid");
    }

    terraformDirectory = normalizeScriptPath(terraformDirectory);
    GitConfig gitConfig = (GitConfig) gitSettingAttribute.getValue();
    gitConfigHelperService.setSshKeySettingAttributeIfNeeded(gitConfig);
    gitConfig.setGitRepoType(GitRepositoryType.TERRAFORM);
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(accountId)
            .appId(appId)
            .data(TaskData.builder()
                      .taskType(TaskType.TERRAFORM_INPUT_VARIABLES_OBTAIN_TASK.name())
                      .parameters(new Object[] {
                          TerraformProvisionParameters.builder()
                              .scriptPath(terraformDirectory)
                              .sourceRepoSettingId(gitSettingAttribute.getUuid())
                              .sourceRepo(gitConfig)
                              .sourceRepoEncryptionDetails(secretManager.getEncryptionDetails(gitConfig, appId, null))
                              .sourceRepoBranch(sourceRepoBranch)
                              .build()})
                      .timeout(TimeUnit.SECONDS.toMillis(30))
                      .build())
            .async(false)
            .build();

    ResponseData notifyResponseData;
    try {
      notifyResponseData = delegateService.executeTask(delegateTask);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException("Thread was interrupted. Please try again.");
    }
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      throw new WingsException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else if ((notifyResponseData instanceof RemoteMethodReturnValueData)
        && (((RemoteMethodReturnValueData) notifyResponseData).getException() instanceof InvalidRequestException)) {
      throw(InvalidRequestException)((RemoteMethodReturnValueData) notifyResponseData).getException();
    } else if (!(notifyResponseData instanceof TerraformInputVariablesTaskResponse)) {
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("message", "Unknown Response from delegate")
          .addContext(ResponseData.class, notifyResponseData);
    }

    TerraformInputVariablesTaskResponse taskResponse = (TerraformInputVariablesTaskResponse) notifyResponseData;
    if (taskResponse.getTerraformExecutionData().getExecutionStatus() == ExecutionStatus.SUCCESS) {
      return taskResponse.getVariablesList();
    } else {
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("message", taskResponse.getTerraformExecutionData().getErrorMessage());
    }
  }

  @Override
  public List<String> getTerraformTargets(String appId, String accountId, String provisionerId) {
    InfrastructureProvisioner infrastructureProvisioner = get(appId, provisionerId);
    if (infrastructureProvisioner == null) {
      throw new InvalidRequestException("Infra Provisioner not found");
    }
    if (!(infrastructureProvisioner instanceof TerraformInfrastructureProvisioner)) {
      throw new InvalidRequestException("Targets only valid for Terraform Infrastructure Provisioner");
    }
    TerraformInfrastructureProvisioner terraformInfrastructureProvisioner =
        (TerraformInfrastructureProvisioner) infrastructureProvisioner;
    validateProvisioner(terraformInfrastructureProvisioner);
    if (isTemplatizedProvisioner(terraformInfrastructureProvisioner)) {
      throw new WingsException(ErrorCode.INVALID_TERRAFORM_TARGETS_REQUEST);
    }
    SettingAttribute settingAttribute = settingService.get(terraformInfrastructureProvisioner.getSourceRepoSettingId());
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof GitConfig)) {
      throw new InvalidRequestException("Invalid Git Repo");
    }
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(accountId)
            .appId(appId)
            .data(TaskData.builder()
                      .taskType(TaskType.TERRAFORM_FETCH_TARGETS_TASK.name())
                      .parameters(new Object[] {
                          TerraformProvisionParameters.builder()
                              .sourceRepoSettingId(settingAttribute.getUuid())
                              .sourceRepo(gitConfig)
                              .sourceRepoBranch(terraformInfrastructureProvisioner.getSourceRepoBranch())
                              .scriptPath(normalizeScriptPath(terraformInfrastructureProvisioner.getPath()))
                              .sourceRepoEncryptionDetails(secretManager.getEncryptionDetails(gitConfig, appId, null))
                              .build()})
                      .timeout(TimeUnit.SECONDS.toMillis(30))
                      .build())
            .async(false)
            .build();
    ResponseData responseData;
    try {
      responseData = delegateService.executeTask(delegateTask);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WingsException("Thread was interrupted. Please try again.");
    }
    if (responseData instanceof ErrorNotifyResponseData) {
      throw new WingsException(((ErrorNotifyResponseData) responseData).getErrorMessage());
    } else if (responseData instanceof RemoteMethodReturnValueData
        && ((RemoteMethodReturnValueData) responseData).getException() instanceof InvalidRequestException) {
      throw(InvalidRequestException)((RemoteMethodReturnValueData) responseData).getException();
    } else if (!(responseData instanceof TerraformExecutionData)) {
      throw new WingsException("Unknown response from delegate.").addContext(ResponseData.class, responseData);
    }
    return ((TerraformExecutionData) responseData).getTargets();
  }

  @Override
  public boolean isTemplatizedProvisioner(TerraformInfrastructureProvisioner infrastructureProvisioner) {
    return (isNotEmpty(infrastructureProvisioner.getSourceRepoBranch())
               && infrastructureProvisioner.getSourceRepoBranch().charAt(0) == '$')
        || (isNotEmpty(infrastructureProvisioner.getPath()) && infrastructureProvisioner.getPath().charAt(0) == '$');
  }

  private void validateProvisioner(TerraformInfrastructureProvisioner terraformInfrastructureProvisioner) {
    if (isEmpty(terraformInfrastructureProvisioner.getSourceRepoBranch())) {
      throw new InvalidRequestException("Provisioner Branch cannot be empty");
    } else if (terraformInfrastructureProvisioner.getPath() == null) {
      throw new InvalidRequestException("Provisioner path cannot be null");
    } else if (isEmpty(terraformInfrastructureProvisioner.getSourceRepoSettingId())) {
      throw new InvalidRequestException("Provisioner should have a source repo");
    }
  }

  private String normalizeScriptPath(String terraformDirectory) {
    return FilenameUtils.normalize(terraformDirectory);
  }

  @Override
  public StreamingOutput downloadTerraformState(String provisionerId, String envId) {
    String entityId = provisionerId + "-" + envId;
    String latestFileId = fileService.getLatestFileId(entityId, FileBucket.TERRAFORM_STATE);
    if (latestFileId == null) {
      throw new InvalidRequestException("No state file found");
    }
    return outputStream -> fileService.downloadToStream(latestFileId, outputStream, FileBucket.TERRAFORM_STATE);
  }

  @Override
  public ShellScriptInfrastructureProvisioner getShellScriptProvisioner(String appId, String provisionerId) {
    InfrastructureProvisioner infrastructureProvisioner = get(appId, provisionerId);
    if (!(infrastructureProvisioner instanceof ShellScriptInfrastructureProvisioner)) {
      throw new InvalidRequestException("Provisioner not of type Shell Script");
    }
    return (ShellScriptInfrastructureProvisioner) infrastructureProvisioner;
  }

  @Override
  public Map<String, String> extractTextVariables(List<NameValuePair> variables, ExecutionContext context) {
    if (isEmpty(variables)) {
      return Collections.emptyMap();
    }
    return variables.stream()
        .filter(entry -> entry.getValue() != null)
        .filter(entry -> "TEXT".equals(entry.getValueType()))
        .collect(toMap(NameValuePair::getName, entry -> context.renderExpression(entry.getValue())));
  }

  @Override
  public Map<String, EncryptedDataDetail> extractEncryptedTextVariables(List<NameValuePair> variables, String appId) {
    if (isEmpty(variables)) {
      return Collections.emptyMap();
    }
    String accountId = appService.getAccountIdByAppId(appId);
    return variables.stream()
        .filter(entry -> entry.getValue() != null)
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
              secretManager.getSecretManager(accountId, encryptedData.getKmsId(), encryptedData.getEncryptionType());

          return EncryptedDataDetail.builder()
              .encryptionType(encryptedData.getEncryptionType())
              .encryptedData(encryptedData)
              .encryptionConfig(encryptionConfig)
              .build();
        }));
  }

  @Override
  public String getEntityId(String provisionerId, String envId) {
    return provisionerId + "-" + envId;
  }

  @Override
  public ManagerExecutionLogCallback getManagerExecutionCallback(
      String appId, String activityId, String commandUnitName) {
    Log.Builder logBuilder =
        Log.Builder.aLog().withCommandUnitName(commandUnitName).withAppId(appId).withActivityId(activityId);
    return new ManagerExecutionLogCallback(logService, logBuilder, activityId);
  }
}
