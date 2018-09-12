package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.persistence.HIterator;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.Event.Type;
import software.wings.beans.GitConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerDetails;
import software.wings.beans.InfrastructureProvisionerDetails.InfrastructureProvisionerDetailsBuilder;
import software.wings.beans.NameValuePair;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.PageRequestBuilder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.expression.ExpressionEvaluator;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsCFHelperServiceManager;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.utils.Misc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class InfrastructureProvisionerServiceImpl implements InfrastructureProvisionerService {
  private static final Logger logger = LoggerFactory.getLogger(InfrastructureProvisionerServiceImpl.class);

  @Inject private ExpressionEvaluator evaluator;

  @Inject InfrastructureMappingService infrastructureMappingService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject SettingsService settingService;
  @Inject AwsCFHelperServiceManager awsCFHelperServiceManager;
  @Inject private AppService appService;
  @Inject YamlPushService yamlPushService;

  @Inject private WingsPersistence wingsPersistence;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Override
  @ValidationGroups(Create.class)
  public InfrastructureProvisioner save(@Valid InfrastructureProvisioner infrastructureProvisioner) {
    InfrastructureProvisioner finalInfraProvisioner =
        wingsPersistence.saveAndGet(InfrastructureProvisioner.class, infrastructureProvisioner);

    String accountId = appService.getAccountIdByAppId(infrastructureProvisioner.getAppId());
    yamlPushService.pushYamlChangeSet(
        accountId, null, infrastructureProvisioner, Type.CREATE, infrastructureProvisioner.isSyncFromGit(), false);

    return finalInfraProvisioner;
  }

  @Override
  @ValidationGroups(Update.class)
  public InfrastructureProvisioner update(@Valid InfrastructureProvisioner infrastructureProvisioner) {
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

    if (serviceId != null) {
      requestBuilder.addFilter(InfrastructureMappingBlueprint.SERVICE_ID_KEY, Operator.EQ, serviceId);
    }
    if (deploymentType != null) {
      requestBuilder.addFilter(InfrastructureMappingBlueprint.DEPLOYMENT_TYPE_KEY, Operator.EQ, deploymentType);
    }
    if (cloudProviderType != null) {
      requestBuilder.addFilter(InfrastructureMappingBlueprint.CLOUD_PROVIDER_TYPE_KEY, Operator.EQ, cloudProviderType);
    }

    final PageRequest blueprintRequest = requestBuilder.build();
    requestBuilder = aPageRequest().addFilter(InfrastructureProvisioner.APP_ID_KEY, Operator.EQ, appId);

    if (infrastructureProvisionerType != null) {
      requestBuilder.addFilter(
          InfrastructureProvisioner.INFRASTRUCTURE_PROVISIONER_TYPE_KEY, Operator.EQ, infrastructureProvisionerType);
    }

    if (isNotEmpty(blueprintRequest.getFilters())) {
      requestBuilder.addFilter(
          InfrastructureProvisioner.MAPPING_BLUEPRINTS_KEY, Operator.ELEMENT_MATCH, blueprintRequest);
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
    return wingsPersistence.get(InfrastructureProvisioner.class, appId, infrastructureProvisionerId);
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

    if (infrastructureProvisioner == null) {
      return;
    }

    ensureSafeToDelete(appId, infrastructureProvisionerId);

    String accountId = appService.getAccountIdByAppId(infrastructureProvisioner.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, infrastructureProvisioner, null, Type.DELETE, syncFromGit, false);

    wingsPersistence.delete(InfrastructureProvisioner.class, appId, infrastructureProvisionerId);
  }

  @Override
  public void pruneDescendingEntities(String appId, String infrastructureProvisionerId) {}

  private void prune(String appId, String infraProvisionerId) {
    PruneEntityJob.addDefaultJob(
        jobScheduler, InfrastructureProvisioner.class, appId, infraProvisionerId, ofSeconds(5), ofSeconds(15));

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
      List<NameValuePair> properties, Optional<ManagerExecutionLogCallback> executionLogCallbackOptional,
      Optional<String> region, NodeFilteringType nodeFilteringType) {
    final Map<String, Object> stringMap = new HashMap<>();

    generateMapToUpdateInfraMapping(contextMap, properties, executionLogCallbackOptional, stringMap, region);

    try {
      infrastructureMapping.applyProvisionerVariables(stringMap, nodeFilteringType);
      infrastructureMappingService.update(infrastructureMapping);
    } catch (Exception e) {
      addToExecutionLog(executionLogCallbackOptional, Misc.getMessage(e));
      throw e;
    }
  }

  private void generateMapToUpdateInfraMapping(Map<String, Object> contextMap, List<NameValuePair> properties,
      Optional<ManagerExecutionLogCallback> executionLogCallbackOptional, Map<String, Object> stringMap,
      Optional<String> region) {
    for (NameValuePair property : properties) {
      if (property.getValue() == null) {
        continue;
      }
      Object evaluated = null;
      try {
        evaluated = evaluator.evaluate(property.getValue(), contextMap);
      } catch (Exception exception) {
        String errorMsg =
            String.format("The infrastructure provisioner mapping value %s was not resolved from the provided outputs",
                property.getName());
        addToExecutionLog(executionLogCallbackOptional, errorMsg);
        throw new InvalidRequestException(errorMsg, exception, USER);
      }
      if (evaluated == null) {
        String errorMsg =
            String.format("The infrastructure provisioner mapping value %s was not resolved from the provided outputs",
                property.getName());
        addToExecutionLog(executionLogCallbackOptional, errorMsg);
        throw new InvalidRequestException(errorMsg);
      }
      stringMap.put(property.getName(), evaluated);
    }
    region.ifPresent(s -> stringMap.put("region", s));
  }

  private void addToExecutionLog(Optional<ManagerExecutionLogCallback> executionLogCallbackOptional, String errorMsg) {
    if (executionLogCallbackOptional.isPresent()) {
      executionLogCallbackOptional.get().saveExecutionLog("# " + errorMsg);
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

    try (HIterator<InfrastructureMapping> infrastructureMappings =
             new HIterator<>(wingsPersistence.createQuery(InfrastructureMapping.class)
                                 .filter(InfrastructureMapping.APP_ID_KEY, infrastructureProvisioner.getAppId())
                                 .filter(InfrastructureMapping.PROVISIONER_ID_KEY, infrastructureProvisioner.getUuid())
                                 .fetch())) {
      while (infrastructureMappings.hasNext()) {
        InfrastructureMapping infrastructureMapping = infrastructureMappings.next();

        infrastructureProvisioner.getMappingBlueprints()
            .stream()
            .filter(blueprint -> blueprint.getServiceId().equals(infrastructureMapping.getServiceId()))
            .filter(blueprint
                -> blueprint.infrastructureMappingType().name().equals(infrastructureMapping.getInfraMappingType()))
            .forEach(blueprint -> {
              logger.info("Provisioner {} updates infrastructureMapping {}", infrastructureProvisioner.getUuid(),
                  infrastructureMapping.getUuid());
              addToExecutionLog(executionLogCallbackOptional,
                  "Provisioner " + infrastructureProvisioner.getUuid() + " updates infrastructureMapping "
                      + infrastructureMapping.getUuid());
              applyProperties(contextMap, infrastructureMapping, blueprint.getProperties(),
                  executionLogCallbackOptional, region, blueprint.getNodeFilteringType());
            });
      }
    }
  }

  @Override
  public List<AwsCFTemplateParamsData> getCFTemplateParamKeys(
      String type, String region, String awsConfigId, String data) {
    return awsCFHelperServiceManager.getParamsData(type, data, awsConfigId, region);
  }
}
