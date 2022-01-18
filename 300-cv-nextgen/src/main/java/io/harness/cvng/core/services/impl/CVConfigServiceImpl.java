/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.core.entities.CVConfig.CVConfigUpdatableEntity;
import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.cvng.core.services.api.UpdatableEntity;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.encryption.Scope;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import java.time.Duration;
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
  @Inject private Map<DataSourceType, CVConfigUpdatableEntity> dataSourceTypeCVConfigMapBinder;

  @Override
  public CVConfig save(CVConfig cvConfig) {
    checkArgument(cvConfig.getUuid() == null, "UUID should be null when creating CVConfig");
    cvConfig.validate();
    hPersistence.save(cvConfig);
    verificationTaskService.createLiveMonitoringVerificationTask(
        cvConfig.getAccountId(), cvConfig.getUuid(), cvConfig.getType());
    return cvConfig;
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
    Preconditions.checkNotNull(dataSourceTypeCVConfigMapBinder.containsKey(cvConfig.getType()));
    cvConfig.validate();
    UpdateOperations<CVConfig> updateOperations = hPersistence.createUpdateOperations(CVConfig.class);
    UpdatableEntity<CVConfig, CVConfig> updatableEntity = dataSourceTypeCVConfigMapBinder.get(cvConfig.getType());
    updatableEntity.setUpdateOperations(updateOperations, cvConfig);
    hPersistence.update(get(cvConfig.getUuid()), updateOperations);
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
    deletedCVConfigService.save(
        DeletedCVConfig.builder().cvConfig(cvConfig).accountId(cvConfig.getAccountId()).build(), Duration.ofHours(2));
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
      String accountId, String orgIdentifier, String projectIdentifier, String monitoringSourceIdentifier) {
    Query<CVConfig> query = hPersistence.createQuery(CVConfig.class, excludeAuthority)
                                .filter(CVConfigKeys.accountId, accountId)
                                .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
                                .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
                                .filter(CVConfigKeys.identifier, monitoringSourceIdentifier);
    return query.asList();
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
    if (isEmpty(configsForFilter)) {
      return Collections.emptyList();
    }
    List<CVConfig> configsToReturn = new ArrayList<>();
    configsForFilter.forEach(config -> {
      EnvironmentResponseDTO environment =
          nextGenService.getEnvironment(accountId, orgIdentifier, projectIdentifier, config.getEnvIdentifier());
      Preconditions.checkNotNull(environment, "no env with identifier %s found for account %s org %s project %s",
          config.getEnvIdentifier(), accountId, orgIdentifier, projectIdentifier);
      if (environment.getType().equals(EnvironmentType.Production)) {
        configsToReturn.add(config);
      }
    });
    return configsToReturn;
  }

  @Override
  public List<CVConfig> list(ServiceEnvironmentParams serviceEnvironmentParams) {
    Query<CVConfig> query = createQuery(serviceEnvironmentParams);
    return query.asList();
  }

  @Override
  public List<CVConfig> list(ServiceEnvironmentParams serviceEnvironmentParams, List<String> identifiers) {
    Query<CVConfig> query = createQuery(serviceEnvironmentParams);
    query.field(CVConfigKeys.identifier).in(identifiers);
    return query.asList();
  }

  @Override
  public List<CVConfig> list(ProjectParams projectParams, List<String> identifiers) {
    return hPersistence.createQuery(CVConfig.class, excludeAuthority)
        .filter(CVConfigKeys.accountId, projectParams.getAccountIdentifier())
        .filter(CVConfigKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(CVConfigKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .field(CVConfigKeys.identifier)
        .in(identifiers)
        .asList();
  }

  @Override
  public Map<String, DataSourceType> getDataSourceTypeForCVConfigs(
      ServiceEnvironmentParams serviceEnvironmentParams, List<String> cvConfigIds) {
    Map<String, DataSourceType> cvConfigIdDataSourceTypeMap = new HashMap<>();
    Query<CVConfig> query = createQuery(serviceEnvironmentParams);
    query.asList().forEach(cvConfig -> cvConfigIdDataSourceTypeMap.put(cvConfig.getUuid(), cvConfig.getType()));
    return cvConfigIdDataSourceTypeMap;
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

  private Query createQuery(ServiceEnvironmentParams serviceEnvironmentParams) {
    return hPersistence.createQuery(CVConfig.class, excludeAuthority)
        .filter(CVConfigKeys.accountId, serviceEnvironmentParams.getAccountIdentifier())
        .filter(CVConfigKeys.orgIdentifier, serviceEnvironmentParams.getOrgIdentifier())
        .filter(CVConfigKeys.projectIdentifier, serviceEnvironmentParams.getProjectIdentifier())
        .filter(CVConfigKeys.serviceIdentifier, serviceEnvironmentParams.getServiceIdentifier())
        .filter(CVConfigKeys.envIdentifier, serviceEnvironmentParams.getEnvironmentIdentifier());
  }

  private void deleteConfigsForEntity(
      String accountId, @Nullable String orgIdentifier, @Nullable String projectIdentifier) {
    Query<CVConfig> query = hPersistence.createQuery(CVConfig.class);
    query = query.filter(CVConfigKeys.accountId, accountId);

    if (orgIdentifier != null) {
      query = query.filter(CVConfigKeys.orgIdentifier, orgIdentifier);
    }

    if (projectIdentifier != null) {
      query = query.filter(CVConfigKeys.projectIdentifier, projectIdentifier);
    }

    List<CVConfig> cvConfigs = query.asList();
    cvConfigs.forEach(cvConfig -> delete(cvConfig.getUuid()));
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<CVConfig> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    Preconditions.checkState(clazz.equals(CVConfig.class), "Class should be of type CVConfig");
    this.deleteConfigsForEntity(accountId, orgIdentifier, projectIdentifier);
  }

  @Override
  public void deleteByOrgIdentifier(Class<CVConfig> clazz, String accountId, String orgIdentifier) {
    Preconditions.checkState(clazz.equals(CVConfig.class), "Class should be of type CVConfig");
    this.deleteConfigsForEntity(accountId, orgIdentifier, null);
  }

  @Override
  public void deleteByAccountIdentifier(Class<CVConfig> clazz, String accountId) {
    Preconditions.checkState(clazz.equals(CVConfig.class), "Class should be of type CVConfig");
    this.deleteConfigsForEntity(accountId, null, null);
  }

  @Override
  public List<CVConfig> getExistingMappedConfigs(String accountId, String orgIdentifier, String projectIdentifier,
      String identifier, DataSourceType dataSourceType) {
    List<CVConfig> cvConfigList = hPersistence.createQuery(CVConfig.class, excludeAuthority)
                                      .filter(CVConfigKeys.accountId, accountId)
                                      .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
                                      .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
                                      .field(CVConfigKeys.identifier)
                                      .notEqual(identifier)
                                      .asList();
    return cvConfigList.stream().filter(config -> config.getType().equals(dataSourceType)).collect(Collectors.toList());
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
  public void setHealthMonitoringFlag(
      String accountId, String orgIdentifier, String projectIdentifier, List<String> identifiers, boolean isEnabled) {
    hPersistence.update(hPersistence.createQuery(CVConfig.class)
                            .filter(CVConfigKeys.accountId, accountId)
                            .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
                            .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
                            .field(CVConfigKeys.identifier)
                            .in(identifiers),
        hPersistence.createUpdateOperations(CVConfig.class).set(CVConfigKeys.enabled, isEnabled));
  }

  @Override
  public List<CVConfig> listByMonitoringSources(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String envIdentifier, List<String> monitoringSources) {
    Preconditions.checkNotNull(accountId);
    List<CVConfig> cvConfigs = hPersistence.createQuery(CVConfig.class, excludeAuthority)
                                   .filter(CVConfigKeys.accountId, accountId)
                                   .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
                                   .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
                                   .filter(CVConfigKeys.serviceIdentifier, serviceIdentifier)
                                   .filter(CVConfigKeys.envIdentifier, envIdentifier)
                                   .asList();
    if (monitoringSources == null) {
      return cvConfigs;
    }
    return cvConfigs.stream()
        .filter(cvConfig -> monitoringSources.contains(cvConfig.getIdentifier()))
        .collect(Collectors.toList());
  }

  @Override
  public List<CVConfig> getCVConfigs(ProjectParams projectParams, String identifier) {
    return hPersistence.createQuery(CVConfig.class, excludeAuthority)
        .filter(CVConfigKeys.accountId, projectParams.getAccountIdentifier())
        .filter(CVConfigKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(CVConfigKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(CVConfigKeys.identifier, identifier)
        .asList();
  }
}
