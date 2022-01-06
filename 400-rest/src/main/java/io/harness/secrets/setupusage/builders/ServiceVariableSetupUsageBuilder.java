/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secrets.setupusage.builders;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.expression.SecretString.SECRET_MASK;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.CGConstants.GLOBAL_ENV_ID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.ServiceVariable.ENCRYPTED_VALUE_KEY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SearchFilter;
import io.harness.exception.InvalidArgumentsException;
import io.harness.secrets.setupusage.EncryptionDetail;
import io.harness.secrets.setupusage.SecretSetupUsage;
import io.harness.secrets.setupusage.SecretSetupUsageBuilder;

import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceVariableService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;

@OwnedBy(PL)
@Singleton
public class ServiceVariableSetupUsageBuilder implements SecretSetupUsageBuilder {
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private WingsPersistence wingsPersistence;

  private String getServiceId(@NonNull String serviceTemplateId) {
    return Optional.ofNullable(wingsPersistence.get(ServiceTemplate.class, serviceTemplateId))
        .<InvalidArgumentsException>orElseThrow(() -> {
          throw new InvalidArgumentsException(
              String.format("Can't find service template %s for service variable", serviceTemplateId), USER_SRE);
        })
        .getServiceId();
  }

  private String getEnvId(ServiceVariable serviceVariable) {
    if (serviceVariable.getEntityType() == ENVIRONMENT) {
      Environment environment =
          wingsPersistence.getWithAppId(Environment.class, serviceVariable.getAppId(), serviceVariable.getEntityId());
      return environment == null ? serviceVariable.getEnvId() : environment.getUuid();
    }
    return serviceVariable.getEnvId();
  }

  private List<ServiceVariable> getServiceVariables(Set<String> parentIds, String accountId, String secretId) {
    return serviceVariableService
        .list(aPageRequest()
                  .addFilter(ID_KEY, SearchFilter.Operator.IN, parentIds.toArray())
                  .addFilter(ACCOUNT_ID_KEY, SearchFilter.Operator.EQ, accountId)
                  .addFilter(ENCRYPTED_VALUE_KEY, SearchFilter.Operator.EQ, secretId)
                  .build())
        .getResponse();
  }

  @Override
  public Set<SecretSetupUsage> buildSecretSetupUsages(@NonNull String accountId, @NonNull String secretId,
      @NonNull Map<String, Set<EncryptedDataParent>> parentsByParentIds, @NonNull EncryptionDetail encryptionDetail) {
    Set<String> parentIds = parentsByParentIds.keySet();
    List<ServiceVariable> serviceVariableList = getServiceVariables(parentIds, accountId, secretId);
    Set<SecretSetupUsage> secretSetupUsages = new HashSet<>();
    for (ServiceVariable serviceVariable : serviceVariableList) {
      if (serviceVariable.getEntityType() == EntityType.SERVICE_TEMPLATE) {
        serviceVariable.setServiceId(getServiceId(serviceVariable.getEntityId()));
      }
      serviceVariable.setValue(SECRET_MASK.toCharArray());
      serviceVariable.setEncryptionType(encryptionDetail.getEncryptionType());
      serviceVariable.setEncryptedBy(encryptionDetail.getSecretManagerName());
      secretSetupUsages.add(SecretSetupUsage.builder()
                                .entityId(serviceVariable.getUuid())
                                .type(serviceVariable.getSettingType())
                                .fieldName(ENCRYPTED_VALUE_KEY)
                                .entity(serviceVariable)
                                .build());
    }
    return secretSetupUsages;
  }

  @Override
  public Map<String, Set<String>> buildAppEnvMap(
      String accountId, String secretId, Map<String, Set<EncryptedDataParent>> parentsByParentIds) {
    Set<String> parentIds = parentsByParentIds.keySet();
    List<ServiceVariable> serviceVariables = getServiceVariables(parentIds, accountId, secretId);
    Map<String, Set<String>> appEnvMap = new HashMap<>();
    for (ServiceVariable serviceVariable : serviceVariables) {
      String appId = serviceVariable.getAppId();
      String envId = getEnvId(serviceVariable);
      if (isNotEmpty(appId) && !GLOBAL_APP_ID.equals(appId)) {
        Set<String> envIds = appEnvMap.computeIfAbsent(appId, k -> new HashSet<>());
        if (isNotEmpty(envId) && !GLOBAL_ENV_ID.equals(envId)) {
          envIds.add(envId);
        }
      }
    }
    return appEnvMap;
  }
}
