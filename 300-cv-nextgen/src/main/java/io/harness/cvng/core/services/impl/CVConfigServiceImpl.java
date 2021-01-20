package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.DatasourceTypeDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVEventService;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.beans.EnvToServicesDTO;
import io.harness.encryption.Scope;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class CVConfigServiceImpl implements CVConfigService {
  @Inject private HPersistence hPersistence;
  @Inject private DeletedCVConfigService deletedCVConfigService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private NextGenService nextGenService;
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private CVEventService eventService;

  @Override
  public CVConfig save(CVConfig cvConfig) {
    checkArgument(cvConfig.getUuid() == null, "UUID should be null when creating CVConfig");
    cvConfig.validate();
    hPersistence.save(cvConfig);
    verificationTaskService.create(cvConfig.getAccountId(), cvConfig.getUuid());

    sendScopedCreateEvent(cvConfig);

    return cvConfig;
  }

  private void sendScopedCreateEvent(CVConfig cvConfig) {
    eventService.sendConnectorCreateEvent(cvConfig);
    eventService.sendServiceCreateEvent(cvConfig);
    eventService.sendEnvironmentCreateEvent(cvConfig);
  }

  @Override
  public List<CVConfig> save(List<CVConfig> cvConfigs) {
    return cvConfigs.stream().map(this::save).collect(Collectors.toList());
  }

  @Nullable
  @Override
  public CVConfig get(@NotNull String cvConfigId) {
    return hPersistence.get(CVConfig.class, cvConfigId);
  }

  @Override
  public List<CVConfig> find(String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      String envIdentifier, List<DataSourceType> dataSourceTypes) {
    Preconditions.checkNotNull(accountId);
    List<CVConfig> cvConfigs = hPersistence.createQuery(CVConfig.class, excludeAuthority)
                                   .filter(CVConfigKeys.accountId, accountId)
                                   .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
                                   .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
                                   .filter(CVConfigKeys.serviceIdentifier, serviceIdentifier)
                                   .filter(CVConfigKeys.envIdentifier, envIdentifier)
                                   .asList();
    return cvConfigs.stream()
        .filter(cvConfig -> dataSourceTypes.contains(cvConfig.getType()))
        .collect(Collectors.toList());
  }

  @Override
  public void update(CVConfig cvConfig) {
    checkNotNull(cvConfig.getUuid(), "Trying to update a CVConfig with empty UUID.");
    cvConfig.validate();
    hPersistence.save(cvConfig);
  }

  @Override
  public void update(List<CVConfig> cvConfigs) {
    cvConfigs.forEach(cvConfig -> cvConfig.validate());
    cvConfigs.forEach(this::update); // TODO: implement batch update
  }

  @Override
  public void delete(@NotNull String cvConfigId) {
    CVConfig cvConfig = get(cvConfigId);
    if (cvConfig == null) {
      return;
    }
    deletedCVConfigService.save(DeletedCVConfig.builder()
                                    .cvConfig(cvConfig)
                                    .accountId(cvConfig.getAccountId())
                                    .perpetualTaskId(cvConfig.getPerpetualTaskId())
                                    .build());
    hPersistence.delete(CVConfig.class, cvConfigId);
  }

  @Override
  public void deleteByIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String monitoringSourceIdentifier) {
    hPersistence.createQuery(CVConfig.class, excludeAuthority)
        .filter(CVConfigKeys.accountId, accountId)
        .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
        .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
        .filter(CVConfigKeys.identifier, monitoringSourceIdentifier)
        .forEach(cvConfig -> delete(cvConfig.getUuid()));
  }

  @Override
  public List<CVConfig> findByConnectorIdentifier(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifierWithoutScopePrefix, Scope scope) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(connectorIdentifierWithoutScopePrefix);
    String connectorIdentifier = connectorIdentifierWithoutScopePrefix;
    if (scope == Scope.ACCOUNT || scope == Scope.ORG) {
      connectorIdentifier = scope.getYamlRepresentation() + "." + connectorIdentifierWithoutScopePrefix;
    }
    Query<CVConfig> query = hPersistence.createQuery(CVConfig.class, excludeAuthority)
                                .filter(CVConfigKeys.accountId, accountId)
                                .filter(CVConfigKeys.connectorIdentifier, connectorIdentifier);
    if (scope == Scope.ORG) {
      query = query.filter(CVConfigKeys.orgIdentifier, orgIdentifier);
    }
    if (scope == Scope.PROJECT) {
      query = query.filter(CVConfigKeys.projectIdentifier, projectIdentifier);
    }
    return query.asList();
  }

  @Override
  public List<CVConfig> list(@NotNull String accountId, String connectorIdentifier) {
    Query<CVConfig> query = hPersistence.createQuery(CVConfig.class, excludeAuthority)
                                .filter(CVConfigKeys.accountId, accountId)
                                .filter(CVConfigKeys.connectorIdentifier, connectorIdentifier);
    return query.asList();
  }
  @Override
  public List<CVConfig> list(String accountId, String connectorIdentifier, String productName) {
    Query<CVConfig> query = hPersistence.createQuery(CVConfig.class, excludeAuthority)
                                .filter(CVConfigKeys.accountId, accountId)
                                .filter(CVConfigKeys.connectorIdentifier, connectorIdentifier)
                                .filter(CVConfigKeys.productName, productName);
    return query.asList();
  }
  @Override
  public List<CVConfig> list(
      String accountId, String connectorIdentifier, String productName, String monitoringSourceIdentifier) {
    Query<CVConfig> query = hPersistence.createQuery(CVConfig.class, excludeAuthority)
                                .filter(CVConfigKeys.accountId, accountId)
                                .filter(CVConfigKeys.connectorIdentifier, connectorIdentifier)
                                .filter(CVConfigKeys.productName, productName)
                                .filter(CVConfigKeys.identifier, monitoringSourceIdentifier);
    return query.asList();
  }

  @Override
  public List<EnvToServicesDTO> getEnvToServicesList(String accountId, String orgIdentifier, String projectIdentifier) {
    Map<String, Set<String>> envToServicesMap = getEnvToServicesMap(accountId, orgIdentifier, projectIdentifier);

    List<EnvToServicesDTO> envToServicesDTOS = new ArrayList<>();
    envToServicesMap.forEach((envIdentifier, serviceIdentifiers) -> {
      EnvironmentResponseDTO environment =
          nextGenService.getEnvironment(envIdentifier, accountId, orgIdentifier, projectIdentifier);
      Set<ServiceResponseDTO> services = new HashSet<>();
      serviceIdentifiers.forEach(serviceIdentifier
          -> services.add(nextGenService.getService(serviceIdentifier, accountId, orgIdentifier, projectIdentifier)));

      envToServicesDTOS.add(EnvToServicesDTO.builder().environment(environment).services(services).build());
    });
    return envToServicesDTOS;
  }

  @Override
  public Map<String, Set<String>> getEnvToServicesMap(
      String accountId, String orgIdentifier, String projectIdentifier) {
    List<CVConfig> cvConfigs = listConfigsForProject(accountId, orgIdentifier, projectIdentifier);
    Map<String, Set<String>> envToServicesMap = new HashMap<>();
    cvConfigs.forEach(cvConfig -> {
      if (!envToServicesMap.containsKey(cvConfig.getEnvIdentifier())) {
        envToServicesMap.put(cvConfig.getEnvIdentifier(), new HashSet<>());
      }
      envToServicesMap.get(cvConfig.getEnvIdentifier()).add(cvConfig.getServiceIdentifier());
    });
    return envToServicesMap;
  }

  private List<CVConfig> listConfigsForProject(String accountId, String orgIdentifier, String projectIdentifier) {
    return hPersistence.createQuery(CVConfig.class, excludeAuthority)
        .filter(CVConfigKeys.accountId, accountId)
        .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
        .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
        .asList();
  }

  @Override
  public List<String> getProductNames(String accountId, String connectorIdentifier) {
    checkNotNull(accountId, "accountId can not be null");
    checkNotNull(connectorIdentifier, "ConnectorIdentifier can not be null");
    return hPersistence.createQuery(CVConfig.class, excludeAuthority)
        .filter(CVConfigKeys.connectorIdentifier, connectorIdentifier)
        .filter(CVConfigKeys.accountId, accountId)
        .project(CVConfigKeys.productName, true)
        .asList()
        .stream()
        .map(cvConfig -> cvConfig.getProductName())
        .distinct()
        .sorted()
        .collect(toList());
  }

  @Override
  public void setCollectionTaskId(String cvConfigId, String perpetualTaskId) {
    UpdateOperations<CVConfig> updateOperations =
        hPersistence.createUpdateOperations(CVConfig.class).set(CVConfigKeys.perpetualTaskId, perpetualTaskId);
    Query<CVConfig> query =
        hPersistence.createQuery(CVConfig.class, excludeAuthority).filter(CVConfigKeys.uuid, cvConfigId);
    hPersistence.update(query, updateOperations);
  }

  @Override
  public Set<CVMonitoringCategory> getAvailableCategories(String accountId, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String serviceIdentifier) {
    BasicDBObject cvConfigQuery = getQueryWithAccountOrgProjectFiltersSet(
        accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier);
    Set<CVMonitoringCategory> cvMonitoringCategories = new HashSet<>();
    hPersistence.getCollection(CVConfig.class)
        .distinct(CVConfigKeys.category, cvConfigQuery)
        .forEach(categoryName -> cvMonitoringCategories.add(CVMonitoringCategory.valueOf((String) categoryName)));
    return cvMonitoringCategories;
  }

  private BasicDBObject getQueryWithAccountOrgProjectFiltersSet(String accountId, String orgIdentifier,
      String projectIdentifier, String envIdentifier, String serviceIdentifier) {
    BasicDBObject cvConfigQuery = new BasicDBObject();
    List<BasicDBObject> conditions = new ArrayList<>();
    conditions.add(new BasicDBObject(CVConfigKeys.accountId, accountId));
    conditions.add(new BasicDBObject(CVConfigKeys.projectIdentifier, projectIdentifier));
    conditions.add(new BasicDBObject(CVConfigKeys.orgIdentifier, orgIdentifier));
    if (isNotEmpty(envIdentifier)) {
      conditions.add(new BasicDBObject(CVConfigKeys.envIdentifier, envIdentifier));
    }

    if (isNotEmpty(serviceIdentifier)) {
      conditions.add(new BasicDBObject(CVConfigKeys.serviceIdentifier, serviceIdentifier));
    }
    cvConfigQuery.put("$and", conditions);
    return cvConfigQuery;
  }

  @Override
  public List<CVConfig> list(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentIdentifier, String serviceIdentifier, CVMonitoringCategory monitoringCategory) {
    Query<CVConfig> query = hPersistence.createQuery(CVConfig.class, excludeAuthority)
                                .filter(CVConfigKeys.accountId, accountId)
                                .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
                                .filter(CVConfigKeys.projectIdentifier, projectIdentifier);
    if (isNotEmpty(environmentIdentifier)) {
      query = query.filter(CVConfigKeys.envIdentifier, environmentIdentifier);
    }
    if (isNotEmpty(serviceIdentifier)) {
      query = query.filter(CVConfigKeys.serviceIdentifier, serviceIdentifier);
    }
    if (monitoringCategory != null) {
      query = query.filter(CVConfigKeys.category, monitoringCategory);
    }
    return query.asList();
  }

  @Override
  public List<CVConfig> getConfigsOfProductionEnvironments(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory) {
    List<CVConfig> configsForFilter =
        list(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier, monitoringCategory);
    List<CVConfig> configsToReturn = new ArrayList<>();
    configsForFilter.forEach(config -> {
      EnvironmentResponseDTO environment = nextGenService.getEnvironment(
          config.getEnvIdentifier(), config.getAccountId(), config.getOrgIdentifier(), config.getProjectIdentifier());
      if (environment.getType().equals(EnvironmentType.Production)) {
        configsToReturn.add(config);
      }
    });
    return configsToReturn;
  }

  @Override
  public boolean isProductionConfig(CVConfig cvConfig) {
    EnvironmentResponseDTO environment = nextGenService.getEnvironment(cvConfig.getEnvIdentifier(),
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());
    return EnvironmentType.Production.equals(environment.getType());
  }

  @Override
  public List<CVConfig> getCVConfigs(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier) {
    return hPersistence.createQuery(CVConfig.class, excludeAuthority)
        .filter(CVConfigKeys.accountId, accountId)
        .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
        .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
        .filter(CVConfigKeys.serviceIdentifier, serviceIdentifier)
        .asList();
  }

  @Override
  public List<String> getMonitoringSourceIds(
      String accountId, String orgIdentifier, String projectIdentifier, String filter) {
    BasicDBObject cvConfigQuery = new BasicDBObject();
    List<BasicDBObject> conditions = new ArrayList<>();
    conditions.add(new BasicDBObject(CVConfigKeys.accountId, accountId));
    conditions.add(new BasicDBObject(CVConfigKeys.projectIdentifier, projectIdentifier));
    conditions.add(new BasicDBObject(CVConfigKeys.orgIdentifier, orgIdentifier));
    cvConfigQuery.put("$and", conditions);
    List<String> allMonitoringSourceIds =
        hPersistence.getCollection(CVConfig.class).distinct(CVConfigKeys.identifier, cvConfigQuery);
    Collections.reverse(allMonitoringSourceIds);
    if (isEmpty(allMonitoringSourceIds) || isEmpty(filter)) {
      return allMonitoringSourceIds;
    }

    return allMonitoringSourceIds.stream()
        .filter(identifier -> identifier.toLowerCase().contains(filter.trim().toLowerCase()))
        .collect(toList());
  }

  @Override
  public List<CVConfig> listByMonitoringSources(
      String accountId, String orgIdentifier, String projectIdentifier, List<String> monitoringSourceIdentifier) {
    return hPersistence.createQuery(CVConfig.class, excludeAuthority)
        .filter(CVConfigKeys.accountId, accountId)
        .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
        .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
        .field(CVConfigKeys.identifier)
        .in(monitoringSourceIdentifier)
        .asList();
  }

  @Override
  public boolean doesAnyCVConfigExistsInProject(String accountId, String orgIdentifier, String projectIdentifier) {
    long numberOfCVConfigs = hPersistence.createQuery(CVConfig.class, excludeAuthority)
                                 .filter(CVConfigKeys.accountId, accountId)
                                 .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
                                 .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
                                 .count();
    return numberOfCVConfigs > 0;
  }

  @Override
  public int getNumberOfServicesSetup(String accountId, String orgIdentifier, String projectIdentifier) {
    BasicDBObject cvConfigQuery =
        getQueryWithAccountOrgProjectFiltersSet(accountId, orgIdentifier, projectIdentifier, null, null);
    List<String> serviceIdentifiers =
        hPersistence.getCollection(CVConfig.class).distinct(CVConfigKeys.serviceIdentifier, cvConfigQuery);
    return serviceIdentifiers.size();
  }

  @Override
  public void deleteConfigsForProject(String accountId, String orgIdentifier, String projectIdentifier) {
    List<CVConfig> cvConfigs = hPersistence.createQuery(CVConfig.class)
                                   .filter(CVConfigKeys.accountId, accountId)
                                   .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
                                   .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
                                   .asList();
    cvConfigs.forEach(cvConfig -> delete(cvConfig.getUuid()));
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<CVConfig> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    Preconditions.checkState(clazz.equals(CVConfig.class), "Class should be of type CVConfig");
    this.deleteConfigsForProject(accountId, orgIdentifier, projectIdentifier);
  }
  @Override
  public List<CVConfig> getExistingMappedConfigs(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String identifier) {
    return hPersistence.createQuery(CVConfig.class, excludeAuthority)
        .filter(CVConfigKeys.accountId, accountId)
        .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
        .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
        .filter(CVConfigKeys.connectorIdentifier, connectorIdentifier)
        .field(CVConfigKeys.identifier)
        .notEqual(identifier)
        .asList();
  }

  @Override
  public Set<DatasourceTypeDTO> getDataSourcetypes(String accountId, String projectIdentifier, String orgIdentifier,
      String environmentIdentifier, String serviceIdentifier, CVMonitoringCategory monitoringCategory) {
    List<CVConfig> cvConfigs = getConfigsOfProductionEnvironments(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier, monitoringCategory);

    if (isEmpty(cvConfigs)) {
      return Collections.emptySet();
    }

    return cvConfigs.stream()
        .map(config
            -> DatasourceTypeDTO.builder()
                   .dataSourceType(config.getType())
                   .verificationType(config.getVerificationType())
                   .build())
        .collect(Collectors.toSet());
  }

  @Override
  public List<String> cleanupPerpetualTasks(String accountId, List<String> cvConfigIds) {
    if (isNotEmpty(cvConfigIds)) {
      List<CVConfig> cvConfigs = hPersistence.createQuery(CVConfig.class)
                                     .filter(CVConfigKeys.accountId, accountId)
                                     .field(CVConfigKeys.uuid)
                                     .in(cvConfigIds)
                                     .asList();
      List<String> perpetualTaskIds = cvConfigs.stream().map(CVConfig::getPerpetualTaskId).collect(toList());
      verificationManagerService.deletePerpetualTasks(accountId, perpetualTaskIds);
      Query<CVConfig> cvConfigQuery = hPersistence.createQuery(CVConfig.class).field(CVConfigKeys.uuid).in(cvConfigIds);
      UpdateOperations<CVConfig> cvConfigUpdateOperations =
          hPersistence.createUpdateOperations(CVConfig.class).unset(CVConfigKeys.perpetualTaskId);
      hPersistence.update(cvConfigQuery, cvConfigUpdateOperations);
      log.info("Cleaned up perpetual tasks for the following cvConfigs : " + cvConfigIds);
    }
    return cvConfigIds;
  }
}
