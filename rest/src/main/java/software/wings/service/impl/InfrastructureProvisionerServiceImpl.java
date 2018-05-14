package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DBCursor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.MorphiaIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.beans.GitConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
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
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  @Inject private WingsPersistence wingsPersistence;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Override
  @ValidationGroups(Create.class)
  public InfrastructureProvisioner save(@Valid InfrastructureProvisioner infrastructureProvisioner) {
    return wingsPersistence.saveAndGet(InfrastructureProvisioner.class, infrastructureProvisioner);
  }

  @Override
  @ValidationGroups(Update.class)
  public InfrastructureProvisioner update(@Valid InfrastructureProvisioner infrastructureProvisioner) {
    return wingsPersistence.saveAndGet(InfrastructureProvisioner.class, infrastructureProvisioner);
  }

  @Override
  public PageResponse<InfrastructureProvisioner> list(PageRequest<InfrastructureProvisioner> pageRequest) {
    return wingsPersistence.query(InfrastructureProvisioner.class, pageRequest);
  }

  @Override
  public PageResponse<InfrastructureProvisioner> listForTask(@NotEmpty String appId,
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
    if (get(appId, infrastructureProvisionerId) == null) {
      return;
    }

    ensureSafeToDelete(appId, infrastructureProvisionerId);
    wingsPersistence.delete(InfrastructureProvisioner.class, appId, infrastructureProvisionerId);
  }

  @Override
  public void pruneDescendingEntities(String appId, String infrastructureProvisionerId) {}

  private void prune(String appId, String infraProvisionerId) {
    PruneEntityJob.addDefaultJob(
        jobScheduler, InfrastructureProvisioner.class, appId, infraProvisionerId, Duration.ofSeconds(5));

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

  private void applyProperties(
      Map<String, Object> contextMap, InfrastructureMapping infrastructureMapping, List<NameValuePair> properties) {
    final Map<String, Object> stringMap = new HashMap<>();
    for (NameValuePair property : properties) {
      final Object evaluated = evaluator.evaluate(property.getValue(), contextMap);
      if (evaluated == null) {
        throw new InvalidRequestException(
            String.format("The infrastructure provisioner mapping value %s was not resolved from the provided outputs",
                property.getName()));
      }
      stringMap.put(property.getName(), evaluated);
    }

    infrastructureMapping.applyProvisionerVariables(stringMap);
    infrastructureMappingService.update(infrastructureMapping);
  }

  @Override
  public void regenerateInfrastructureMappings(
      String provisionerId, ExecutionContext context, Map<String, Object> outputs) {
    final InfrastructureProvisioner infrastructureProvisioner = get(context.getAppId(), provisionerId);

    final Map<String, Object> contextMap = context.asMap();
    contextMap.put(infrastructureProvisioner.variableKey(), outputs);

    final MorphiaIterator<InfrastructureMapping, InfrastructureMapping> infrastructureMappings =
        wingsPersistence.createQuery(InfrastructureMapping.class)
            .filter(InfrastructureMapping.APP_ID_KEY, infrastructureProvisioner.getAppId())
            .filter(InfrastructureMapping.PROVISIONER_ID_KEY, infrastructureProvisioner.getUuid())
            .fetch();

    try (DBCursor cursor = infrastructureMappings.getCursor()) {
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
              applyProperties(contextMap, infrastructureMapping, blueprint.getProperties());
            });
      }
    }
  }
}
