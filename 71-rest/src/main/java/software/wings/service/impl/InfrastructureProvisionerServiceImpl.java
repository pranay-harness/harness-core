package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.atteo.evo.inflector.English.plural;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.context.ContextElementType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitEnforcementUtils;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.persistence.HIterator;
import io.harness.queue.QueuePublisher;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.validation.Create;
import io.harness.validation.Update;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.Key;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.BlueprintProperty;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.CloudFormationSourceType;
import software.wings.beans.EntityType;
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
import software.wings.beans.Log.Builder;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.TaskType;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.TerraformInputVariablesTaskResponse;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.ProvisionerAware;
import software.wings.prune.PruneEvent;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.aws.manager.AwsCFHelperServiceManager;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.states.ManagerExecutionLogCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;
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
  @Inject private QueuePublisher<PruneEvent> pruneQueue;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private HarnessTagService harnessTagService;
  @Inject private ResourceLookupService resourceLookupService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Override
  @ValidationGroups(Create.class)
  public InfrastructureProvisioner save(@Valid InfrastructureProvisioner infrastructureProvisioner) {
    String accountId = appService.getAccountIdByAppId(infrastructureProvisioner.getAppId());
    infrastructureProvisioner.setAccountId(accountId);
    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_INFRA_PROVISIONER));

    return LimitEnforcementUtils.withLimitCheck(checker, () -> {
      populateDerivedFields(infrastructureProvisioner);

      removeDuplicateVariables(infrastructureProvisioner);
      validateProvisioner(infrastructureProvisioner);

      InfrastructureProvisioner finalInfraProvisioner =
          wingsPersistence.saveAndGet(InfrastructureProvisioner.class, infrastructureProvisioner);

      yamlPushService.pushYamlChangeSet(
          accountId, null, infrastructureProvisioner, Type.CREATE, infrastructureProvisioner.isSyncFromGit(), false);

      return finalInfraProvisioner;
    });
  }

  void removeDuplicateVariables(InfrastructureProvisioner infrastructureProvisioner) {
    List<NameValuePair> variables = infrastructureProvisioner.getVariables();
    if (isEmpty(variables)) {
      return;
    }
    ArrayList<NameValuePair> distinctVariables = new ArrayList<>(variables.size());
    HashSet<String> distinctVariableNames = new HashSet<>();
    for (NameValuePair variable : variables) {
      if (!distinctVariableNames.contains(variable.getName())) {
        distinctVariables.add(variable);
        distinctVariableNames.add(variable.getName());
      }
    }
    infrastructureProvisioner.setVariables(distinctVariables);
  }

  @VisibleForTesting
  void validateProvisioner(InfrastructureProvisioner provisioner) {
    if (provisioner instanceof TerraformInfrastructureProvisioner) {
      validateTerraformProvisioner((TerraformInfrastructureProvisioner) provisioner);
    }
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

    removeDuplicateVariables(infrastructureProvisioner);
    validateProvisioner(infrastructureProvisioner);

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

  InfrastructureProvisionerDetails details(InfrastructureProvisioner provisioner,
      Map<String, SettingAttribute> idToSettingAttributeMapping, Map<String, Service> idToServiceMapping) {
    final InfrastructureProvisionerDetailsBuilder detailsBuilder =
        InfrastructureProvisionerDetails.builder()
            .uuid(provisioner.getUuid())
            .name(provisioner.getName())
            .description(provisioner.getDescription())
            .infrastructureProvisionerType(provisioner.getInfrastructureProvisionerType())
            .tagLinks(provisioner.getTagLinks());

    if (provisioner instanceof TerraformInfrastructureProvisioner) {
      final TerraformInfrastructureProvisioner terraformInfrastructureProvisioner =
          (TerraformInfrastructureProvisioner) provisioner;

      final SettingAttribute settingAttribute =
          idToSettingAttributeMapping.get(terraformInfrastructureProvisioner.getSourceRepoSettingId());

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

    if (!featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, provisioner.getAccountId())) {
      if (isNotEmpty(provisioner.getMappingBlueprints())) {
        detailsBuilder.services(provisioner.getMappingBlueprints()
                                    .stream()
                                    .map(InfrastructureMappingBlueprint::getServiceId)
                                    .map(idToServiceMapping::get)
                                    .filter(Objects::nonNull)
                                    .collect(toMap(Service::getName, Service::getUuid)));
      }
    }
    return detailsBuilder.build();
  }

  Map<String, Service> getIdToServiceMapping(String appId, Set<String> servicesIds) {
    if (isEmpty(servicesIds)) {
      return Collections.emptyMap();
    }
    PageRequest<Service> servicePageRequest = new PageRequest<>();
    servicePageRequest.addFilter(Service.APP_ID_KEY, Operator.EQ, appId);
    servicePageRequest.addFilter(ServiceKeys.uuid, Operator.IN, servicesIds.toArray());

    PageResponse<Service> services = serviceResourceService.list(servicePageRequest, false, false, false, null);

    Map<String, Service> idToServiceMapping = new HashMap<>();
    for (Service service : services.getResponse()) {
      idToServiceMapping.put(service.getUuid(), service);
    }
    return idToServiceMapping;
  }

  Map<String, SettingAttribute> getIdToSettingAttributeMapping(String accountId, Set<String> settingAttributeIds) {
    if (isEmpty(settingAttributeIds)) {
      return Collections.emptyMap();
    }
    PageRequest<SettingAttribute> settingAttributePageRequest = new PageRequest<>();
    settingAttributePageRequest.addFilter(SettingAttribute.ACCOUNT_ID_KEY, Operator.EQ, accountId);
    settingAttributePageRequest.addFilter(
        SettingAttribute.VALUE_TYPE_KEY, Operator.EQ, SettingVariableTypes.GIT.name());
    settingAttributePageRequest.addFilter(SettingAttributeKeys.uuid, Operator.IN, settingAttributeIds.toArray());

    PageResponse<SettingAttribute> settingAttributes = settingService.list(settingAttributePageRequest, null, null);

    Map<String, SettingAttribute> idToSettingAttributeMapping = new HashMap<>();
    for (SettingAttribute settingAttribute : settingAttributes.getResponse()) {
      idToSettingAttributeMapping.put(settingAttribute.getUuid(), settingAttribute);
    }
    return idToSettingAttributeMapping;
  }

  @Override
  public PageResponse<InfrastructureProvisionerDetails> listDetails(
      PageRequest<InfrastructureProvisioner> pageRequest, boolean withTags, String tagFilter, @NotEmpty String appId) {
    final long apiStartTime = System.currentTimeMillis();
    PageResponse<InfrastructureProvisioner> pageResponse =
        resourceLookupService.listWithTagFilters(pageRequest, tagFilter, EntityType.PROVISIONER, withTags);

    logger.info(
        format("Time taken in fetching listWithTagFilters : [%s] ms", System.currentTimeMillis() - apiStartTime));
    long startTime = System.currentTimeMillis();

    Set<String> settingAttributeIds = new HashSet<>();
    Set<String> servicesIds = new HashSet<>();
    for (InfrastructureProvisioner infrastructureProvisioner : pageResponse.getResponse()) {
      if (infrastructureProvisioner instanceof TerraformInfrastructureProvisioner) {
        settingAttributeIds.add(
            ((TerraformInfrastructureProvisioner) infrastructureProvisioner).getSourceRepoSettingId());
      }
      if (isNotEmpty(infrastructureProvisioner.getMappingBlueprints())) {
        infrastructureProvisioner.getMappingBlueprints()
            .stream()
            .map(InfrastructureMappingBlueprint::getServiceId)
            .forEach(servicesIds::add);
      }
    }

    Map<String, SettingAttribute> idToSettingAttributeMapping =
        getIdToSettingAttributeMapping(appService.getAccountIdByAppId(appId), settingAttributeIds);

    logger.info(
        format("Time taken in getIdToSettingAttributeMapping : [%s] ms", System.currentTimeMillis() - startTime));
    startTime = System.currentTimeMillis();

    Map<String, Service> idToServiceMapping = getIdToServiceMapping(appId, servicesIds);
    logger.info("Time taken in idToServiceMapping : [{}] ms", System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();

    PageResponse<InfrastructureProvisionerDetails> infrastructureProvisionerDetails = new PageResponse<>();
    infrastructureProvisionerDetails.setResponse(
        pageResponse.getResponse()
            .stream()
            .map(item -> details(item, idToSettingAttributeMapping, idToServiceMapping))
            .collect(toList()));
    logger.info("Time taken in setting details : [{}] ms", System.currentTimeMillis() - startTime);
    logger.info("Time taken by details api : [{}] ms", System.currentTimeMillis() - apiStartTime);
    return infrastructureProvisionerDetails;
  }

  @Override
  public InfrastructureProvisioner get(String appId, String infrastructureProvisionerId) {
    InfrastructureProvisioner infrastructureProvisioner =
        wingsPersistence.getWithAppId(InfrastructureProvisioner.class, appId, infrastructureProvisionerId);
    if (infrastructureProvisioner == null) {
      throw new InvalidRequestException("Provisioner Not Found");
    }
    if (!featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, infrastructureProvisioner.getAccountId())) {
      List<InfrastructureMappingBlueprint> mappingBlueprints = infrastructureProvisioner.getMappingBlueprints();
      if (isNotEmpty(mappingBlueprints)) {
        Set<String> serviceIds =
            mappingBlueprints.stream().map(InfrastructureMappingBlueprint::getServiceId).collect(toSet());
        Map<String, Service> idToServiceMapping = getIdToServiceMapping(appId, serviceIds);
        Set<String> existingServiceIds = idToServiceMapping.keySet();
        if (isEmpty(existingServiceIds)) {
          infrastructureProvisioner.setMappingBlueprints(emptyList());
        } else {
          infrastructureProvisioner.setMappingBlueprints(
              mappingBlueprints.stream()
                  .filter(mappingBlueprint -> existingServiceIds.contains(mappingBlueprint.getServiceId()))
                  .collect(toList()));
        }
      }
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

  private void ensureSafeToDelete(String appId, InfrastructureProvisioner infrastructureProvisioner) {
    final String accountId = appService.getAccountIdByAppId(appId);
    final String infrastructureProvisionerId = infrastructureProvisioner.getUuid();

    if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId)) {
      List<String> infraDefinitionNames =
          infrastructureDefinitionService.listNamesByProvisionerId(appId, infrastructureProvisionerId);
      if (isNotEmpty(infraDefinitionNames)) {
        throw new InvalidRequestException(format("Infrastructure provisioner [%s] is not safe to "
                + "delete."
                + "Referenced "
                + "by "
                + "%d %s [%s].",
            infrastructureProvisioner.getName(), infraDefinitionNames.size(),
            plural("Infrastructure "
                    + "Definition",
                infraDefinitionNames.size()),
            HarnessStringUtils.join(", ", infraDefinitionNames)));
      }
    } else {
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
            format("Infrastructure provisioner %s is not safe to delete", infrastructureProvisionerId), exception,
            USER);
      }
    }
  }

  @Override
  public void delete(String appId, String infrastructureProvisionerId) {
    delete(appId, infrastructureProvisionerId, false);
  }

  @Override
  public void delete(String appId, String infrastructureProvisionerId, boolean syncFromGit) {
    InfrastructureProvisioner infrastructureProvisioner = get(appId, infrastructureProvisionerId);

    ensureSafeToDelete(appId, infrastructureProvisioner);

    String accountId = appService.getAccountIdByAppId(infrastructureProvisioner.getAppId());
    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_INFRA_PROVISIONER));

    LimitEnforcementUtils.withCounterDecrement(checker, () -> {
      ensureSafeToDelete(appId, infrastructureProvisioner);

      yamlPushService.pushYamlChangeSet(accountId, infrastructureProvisioner, null, Type.DELETE, syncFromGit, false);

      wingsPersistence.delete(InfrastructureProvisioner.class, appId, infrastructureProvisionerId);
    });
  }

  @Override
  public void pruneDescendingEntities(String appId, String infrastructureProvisionerId) {
    // nothing to prune
  }

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
      harnessTagService.pruneTagLinks(appService.getAccountIdByAppId(appId), (String) key.getId());
    }
  }

  // Region is optional and is not present in the blue print properties for cloud formation provisioners and
  // present for terraform
  private void applyProperties(Map<String, Object> contextMap, InfrastructureMapping infrastructureMapping,
      List<BlueprintProperty> properties, Optional<ManagerExecutionLogCallback> executionLogCallbackOptional,
      Optional<String> region, NodeFilteringType nodeFilteringType, String infraProvisionerTypeKey) {
    Map<String, Object> propertyNameEvaluatedMap =
        resolveProperties(contextMap, properties, executionLogCallbackOptional, region, false, infraProvisionerTypeKey);

    try {
      infrastructureMapping.applyProvisionerVariables(propertyNameEvaluatedMap, nodeFilteringType, false);
      infrastructureMappingService.update(infrastructureMapping);
    } catch (Exception e) {
      addToExecutionLog(executionLogCallbackOptional, ExceptionUtils.getMessage(e));
      throw e;
    }
  }

  @Override
  public Map<String, Object> resolveProperties(Map<String, Object> contextMap, List<BlueprintProperty> properties,
      Optional<ManagerExecutionLogCallback> executionLogCallbackOptional, Optional<String> region,
      boolean infraRefactor, String infraProvisionerTypeKey) {
    Map<String, Object> propertyNameEvaluatedMap = getPropertyNameEvaluatedMap(
        properties.stream()
            .map(property -> new NameValuePair(property.getName(), property.getValue(), property.getValueType()))
            .collect(toList()),
        contextMap, infraRefactor, infraProvisionerTypeKey);

    for (BlueprintProperty property : properties) {
      if (isNotEmpty(property.getFields())) {
        String propertyName = property.getName();
        Object evaluatedValue = propertyNameEvaluatedMap.get(propertyName);
        if (evaluatedValue == null) {
          throw new InvalidRequestException(format("Property %s not found for resolving fields", propertyName));
        }
        List<NameValuePair> fields = property.getFields();
        if (evaluatedValue instanceof List) {
          List<Map<String, Object>> fieldsEvaluatedList = new ArrayList<>();
          for (Object evaluatedEntry : (List) evaluatedValue) {
            fieldsEvaluatedList.add(getPropertyNameEvaluatedMap(
                fields, (Map<String, Object>) evaluatedEntry, infraRefactor, infraProvisionerTypeKey));
          }
          propertyNameEvaluatedMap.put(propertyName, fieldsEvaluatedList);

        } else {
          getPropertyNameEvaluatedMap(
              fields, (Map<String, Object>) evaluatedValue, infraRefactor, infraProvisionerTypeKey);
        }
      }
    }

    region.ifPresent(s -> propertyNameEvaluatedMap.put("region", s));
    return propertyNameEvaluatedMap;
  }

  @NotNull
  @VisibleForTesting
  Map<String, Object> getPropertyNameEvaluatedMap(List<NameValuePair> properties, Map<String, Object> contextMap,
      boolean infraRefactor, String infrastructureProvisionerTypeKey) {
    Map<String, Object> propertyNameEvaluatedMap = new HashMap<>();
    for (NameValuePair property : properties) {
      if (isEmpty(property.getValue())) {
        continue;
      }
      if (infraRefactor && !property.getValue().contains("$")) {
        propertyNameEvaluatedMap.put(property.getName(), property.getValue());
        continue;
      }
      Object evaluated = null;
      try {
        evaluated = evaluator.evaluate(property.getValue(), contextMap);
      } catch (Exception ignore) {
        // ignore this exception, it is based on user input
      }
      if (evaluated == null) {
        if (!infraRefactor || property.getValue().contains(format("${%s.", infrastructureProvisionerTypeKey))) {
          throw new InvalidRequestException(format("Unable to resolve \"%s\" ", property.getValue()), USER);
        }
        logger.info("Unresolved expression \"{}\" ", property.getValue());
        evaluated = property.getValue();
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
    String appId = context.getAppId();
    final InfrastructureProvisioner infrastructureProvisioner = get(appId, provisionerId);

    final Map<String, Object> contextMap = context.asMap();
    contextMap.put(infrastructureProvisioner.variableKey(), outputs);
    contextMap.putAll(outputs);

    boolean featureFlagEnabled =
        featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, infrastructureProvisioner.getAccountId());

    if (featureFlagEnabled) {
      try {
        String infraMappingId = getInfraMappingId(context);
        String infraDefinitionId = getInfraDefinitionId(context);
        if (isEmpty(infraMappingId) && isNotEmpty(infraDefinitionId)) {
          // Inside deployment phase, but mapping not yet generated
          InfrastructureDefinition infrastructureDefinition =
              infrastructureDefinitionService.get(appId, infraDefinitionId);
          addToExecutionLog(executionLogCallbackOptional,
              format("Mapping provisioner outputs to Infra Definition: [%s]", infrastructureDefinition.getName()));
          Map<String, Object> resolvedExpressions =
              resolveExpressions(infrastructureDefinition, contextMap, infrastructureProvisioner);
          ((ProvisionerAware) infrastructureDefinition.getInfrastructure())
              .applyExpressions(resolvedExpressions, appId, infrastructureDefinition.getEnvId(), infraDefinitionId);
          InfrastructureMapping infrastructureMapping =
              infrastructureDefinitionService.getInfrastructureMapping(getServiceId(context), infrastructureDefinition);
          PhaseElement phaseElement =
              context.getContextElement(ContextElementType.PARAM, ExecutionContextImpl.PHASE_PARAM);
          infrastructureMappingService.saveInfrastructureMappingToSweepingOutput(
              appId, context.getWorkflowExecutionId(), phaseElement, infrastructureMapping.getUuid());
          workflowExecutionService.appendInfraMappingId(
              appId, context.getWorkflowExecutionId(), infrastructureMapping.getUuid());
          addToExecutionLog(executionLogCallbackOptional, "Mapping completed successfully");
        }
      } catch (WingsException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new UnexpectedException("Failed while mapping provisioner outputs to Infra Definition", ex);
      }
    } else {
      try (HIterator<InfrastructureMapping> infrastructureMappings = new HIterator<>(
               wingsPersistence.createQuery(InfrastructureMapping.class)
                   .filter(InfrastructureMapping.APP_ID_KEY, infrastructureProvisioner.getAppId())
                   .filter(InfrastructureMapping.PROVISIONER_ID_KEY, infrastructureProvisioner.getUuid())
                   .fetch())) {
        if (infrastructureMappings.hasNext()) {
          while (infrastructureMappings.hasNext()) {
            InfrastructureMapping infrastructureMapping = infrastructureMappings.next();

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
                      executionLogCallbackOptional, region, blueprint.getNodeFilteringType(),
                      infrastructureProvisioner.variableKey());
                });
          }
        }
      }
    }
  }

  private String getServiceId(ExecutionContext context) {
    return ((PhaseElement) context.getContextElement(ContextElementType.PARAM, ExecutionContextImpl.PHASE_PARAM))
        .getServiceElement()
        .getUuid();
  }

  private String getInfraDefinitionId(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, ExecutionContextImpl.PHASE_PARAM);
    return phaseElement == null ? null : phaseElement.getInfraDefinitionId();
  }

  private String getInfraMappingId(ExecutionContext context) {
    return context.fetchInfraMappingId();
  }

  @Override
  public List<AwsCFTemplateParamsData> getCFTemplateParamKeys(String type, String region, String awsConfigId,
      String data, String appId, String sourceRepoSettingId, String sourceRepoBranch, String templatePath,
      String commitId, Boolean useBranch) {
    if (type.equalsIgnoreCase(CloudFormationSourceType.GIT.name())) {
      if (isEmpty(sourceRepoSettingId) || (isEmpty(sourceRepoBranch) && isEmpty(commitId))) {
        throw new InvalidRequestException("Empty Fields Connector Id or both Branch and commitID");
      }
    } else if (isEmpty(data)) {
      throw new InvalidRequestException("Empty Data Field, Template body or Template url");
    }
    return awsCFHelperServiceManager.getParamsData(type, data, awsConfigId, region, appId, sourceRepoSettingId,
        sourceRepoBranch, templatePath, commitId, useBranch);
  }

  @Override
  public List<NameValuePair> getTerraformVariables(
      String appId, String scmSettingId, String terraformDirectory, String accountId, String sourceRepoBranch) {
    SettingAttribute gitSettingAttribute = settingService.get(scmSettingId);
    notNullCheck("Source repo provided is not Valid", gitSettingAttribute);
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
    validateTerraformProvisioner(terraformInfrastructureProvisioner);
    if (isTemplatizedProvisioner(terraformInfrastructureProvisioner)) {
      throw new WingsException(ErrorCode.INVALID_TERRAFORM_TARGETS_REQUEST);
    }
    SettingAttribute settingAttribute = settingService.get(terraformInfrastructureProvisioner.getSourceRepoSettingId());
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof GitConfig)) {
      throw new InvalidRequestException("Invalid Git Repo");
    }
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
    gitConfig.setGitRepoType(GitRepositoryType.TERRAFORM);

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

  private void validateTerraformProvisioner(TerraformInfrastructureProvisioner terraformProvisioner) {
    if (isEmpty(terraformProvisioner.getSourceRepoBranch())) {
      throw new InvalidRequestException("Provisioner Branch cannot be empty");
    } else if (terraformProvisioner.getPath() == null) {
      throw new InvalidRequestException("Provisioner path cannot be null");
    } else if (isEmpty(terraformProvisioner.getSourceRepoSettingId())) {
      throw new InvalidRequestException("Provisioner should have a source repo");
    }

    boolean areVariablesValid =
        areKeysMongoCompliant(terraformProvisioner.getVariables(), terraformProvisioner.getBackendConfigs());
    if (!areVariablesValid) {
      throw new InvalidRequestException("The following characters are not allowed in terraform "
          + "variable names: . and $");
    }
  }

  private boolean areKeysMongoCompliant(List<NameValuePair>... variables) {
    Predicate<String> terraformVariableNameCheckFail = value -> value.contains(".") || value.contains("$");
    return Stream.of(variables)
        .filter(EmptyPredicate::isNotEmpty)
        .flatMap(Collection::stream)
        .map(NameValuePair::getName)
        .noneMatch(terraformVariableNameCheckFail);
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
              .encryptedData(SecretManager.buildRecordData(encryptedData))
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
    Builder logBuilder =
        Builder.aLog().withCommandUnitName(commandUnitName).withAppId(appId).withActivityId(activityId);
    return new ManagerExecutionLogCallback(logService, logBuilder, activityId);
  }

  @Override
  public Map<String, Object> resolveExpressions(InfrastructureDefinition infrastructureDefinition,
      Map<String, Object> contextMap, InfrastructureProvisioner infrastructureProvisioner) {
    List<BlueprintProperty> properties = getBlueprintProperties(infrastructureDefinition);
    addProvisionerKeys(properties, infrastructureProvisioner);
    return resolveProperties(
        contextMap, properties, Optional.empty(), Optional.empty(), true, infrastructureProvisioner.variableKey());
  }

  void addProvisionerKeys(List<BlueprintProperty> properties, InfrastructureProvisioner infrastructureProvisioner) {
    if (infrastructureProvisioner instanceof ShellScriptInfrastructureProvisioner) {
      properties.forEach(property -> {
        property.setValue(format("${%s.%s}", infrastructureProvisioner.variableKey(), property.getValue()));
        property.getFields().forEach(field -> field.setValue(format("${%s}", field.getValue())));
      });
    }
  }

  List<BlueprintProperty> getBlueprintProperties(InfrastructureDefinition infrastructureDefinition) {
    List<BlueprintProperty> properties = new ArrayList<>();
    Map<String, String> expressions =
        ((ProvisionerAware) infrastructureDefinition.getInfrastructure()).getExpressions();
    if (infrastructureDefinition.getInfrastructure() instanceof PhysicalInfra) {
      BlueprintProperty hostArrayPathProperty = BlueprintProperty.builder()
                                                    .name(PhysicalInfra.hostArrayPath)
                                                    .value(expressions.get(PhysicalInfra.hostArrayPath))
                                                    .build();
      List<NameValuePair> fields =
          expressions.entrySet()
              .stream()
              .filter(expression -> !expression.getKey().equals(PhysicalInfra.hostArrayPath))
              .map(expression -> NameValuePair.builder().name(expression.getKey()).value(expression.getValue()).build())
              .collect(toList());
      hostArrayPathProperty.setFields(fields);
      properties.add(hostArrayPathProperty);

    } else {
      expressions.entrySet()
          .stream()
          .map(expression -> BlueprintProperty.builder().name(expression.getKey()).value(expression.getValue()).build())
          .forEach(properties::add);
    }
    return properties;
  }
}
