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

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.CGConstants.GLOBAL_ENV_ID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE_TEMPLATE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SearchFilter;
import io.harness.exception.InvalidArgumentsException;
import io.harness.secrets.setupusage.EncryptionDetail;
import io.harness.secrets.setupusage.SecretSetupUsage;
import io.harness.secrets.setupusage.SecretSetupUsageBuilder;

import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigFileKeys;
import software.wings.beans.Environment;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ConfigService;

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
public class ConfigFileSetupUsageBuilder implements SecretSetupUsageBuilder {
  @Inject private ConfigService configService;
  @Inject private WingsPersistence wingsPersistence;

  private List<ConfigFile> getConfigFiles(Set<String> parentIds, String accountId, String secretId) {
    return configService
        .list(aPageRequest()
                  .addFilter(ID_KEY, SearchFilter.Operator.IN, parentIds.toArray())
                  .addFilter(ACCOUNT_ID_KEY, SearchFilter.Operator.EQ, accountId)
                  .addFilter(ConfigFileKeys.encryptedFileId, SearchFilter.Operator.EQ, secretId)
                  .build())
        .getResponse();
  }

  private String getServiceId(@NonNull String serviceTemplateId) {
    return Optional.ofNullable(wingsPersistence.get(ServiceTemplate.class, serviceTemplateId))
        .<InvalidArgumentsException>orElseThrow(() -> {
          throw new InvalidArgumentsException(
              String.format("Can't find service template %s for service variable", serviceTemplateId), USER_SRE);
        })
        .getServiceId();
  }

  private String getEnvId(ConfigFile configFile) {
    if (configFile.getEntityType() == ENVIRONMENT) {
      Environment environment =
          wingsPersistence.getWithAppId(Environment.class, configFile.getAppId(), configFile.getEntityId());
      return environment == null ? configFile.getEnvId() : environment.getUuid();
    }
    return configFile.getEnvId();
  }

  public Set<SecretSetupUsage> buildSecretSetupUsages(String accountId, String secretId,
      Map<String, Set<EncryptedDataParent>> parentsByParentIds, EncryptionDetail encryptionDetail) {
    Set<String> parentIds = parentsByParentIds.keySet();
    List<ConfigFile> configFileList = getConfigFiles(parentIds, accountId, secretId);
    Set<SecretSetupUsage> secretSetupUsages = new HashSet<>();
    for (ConfigFile configFile : configFileList) {
      if (configFile.getEntityType() == SERVICE_TEMPLATE) {
        configFile.setServiceId(getServiceId(configFile.getEntityId()));
      }
      configFile.setEncryptionType(encryptionDetail.getEncryptionType());
      configFile.setEncryptedBy(encryptionDetail.getSecretManagerName());
      secretSetupUsages.add(SecretSetupUsage.builder()
                                .entityId(configFile.getUuid())
                                .type(configFile.getSettingType())
                                .fieldName(ConfigFileKeys.encryptedFileId)
                                .entity(configFile)
                                .build());
    }
    return secretSetupUsages;
  }

  @Override
  public Map<String, Set<String>> buildAppEnvMap(
      String accountId, String secretId, Map<String, Set<EncryptedDataParent>> parentsByParentIds) {
    Set<String> parentIds = parentsByParentIds.keySet();
    List<ConfigFile> configFileList = getConfigFiles(parentIds, accountId, secretId);
    Map<String, Set<String>> appEnvMap = new HashMap<>();
    for (ConfigFile configFile : configFileList) {
      String appId = configFile.getAppId();
      String envId = getEnvId(configFile);
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
