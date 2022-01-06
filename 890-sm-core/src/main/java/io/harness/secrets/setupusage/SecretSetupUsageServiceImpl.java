/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secrets.setupusage;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER_SRE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedDataParent;
import io.harness.exception.InvalidArgumentsException;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.secrets.SecretsDao;

import software.wings.settings.SettingVariableTypes;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@Slf4j
public class SecretSetupUsageServiceImpl implements SecretSetupUsageService {
  private final SecretsDao secretsDao;
  private final SecretManagerConfigService secretManagerConfigService;
  private final SecretSetupUsageBuilderRegistry secretSetupUsageBuilderRegistry;

  @Inject
  public SecretSetupUsageServiceImpl(SecretsDao secretsDao, SecretManagerConfigService secretManagerConfigService,
      SecretSetupUsageBuilderRegistry secretSetupUsageBuilderRegistry) {
    this.secretsDao = secretsDao;
    this.secretManagerConfigService = secretManagerConfigService;
    this.secretSetupUsageBuilderRegistry = secretSetupUsageBuilderRegistry;
  }

  private void populateParentHelperMaps(Map<SettingVariableTypes, Set<String>> parentIdsByType,
      Map<String, Set<EncryptedDataParent>> parentsByParentId, Set<EncryptedDataParent> parents) {
    parents.forEach(parent -> {
      Set<String> parentIdsTypeSet = parentIdsByType.computeIfAbsent(parent.getType(), k -> new HashSet<>());
      Set<EncryptedDataParent> parentsIdSet = parentsByParentId.computeIfAbsent(parent.getId(), k -> new HashSet<>());
      parentIdsTypeSet.add(parent.getId());
      parentsIdSet.add(parent);
    });
  }

  private Set<SecretSetupUsage> buildSecretSetupUsageFromParentsInternal(@NonNull SettingVariableTypes type,
      @NonNull EncryptionDetail encryptionDetail, @NonNull String accountId, @NonNull String secretId,
      @NonNull Map<String, Set<EncryptedDataParent>> parentsByParentId) {
    Set<SecretSetupUsage> secretSetupUsages = new HashSet<>();
    Optional<SecretSetupUsageBuilder> secretSetupUsageBuilderOptional =
        secretSetupUsageBuilderRegistry.getSecretSetupUsageBuilder(type);
    if (!secretSetupUsageBuilderOptional.isPresent()) {
      log.warn("Building setup usages is not supported for {}", type);
      return secretSetupUsages;
    }
    SecretSetupUsageBuilder secretSetupUsageBuilder = secretSetupUsageBuilderOptional.get();
    return secretSetupUsageBuilder.buildSecretSetupUsages(accountId, secretId, parentsByParentId, encryptionDetail);
  }

  private Set<SecretSetupUsage> buildSecretSetupUsageFromParents(@NonNull String accountId, @NonNull String secretId,
      @NonNull Set<EncryptedDataParent> parents, @NonNull EncryptionDetail encryptionDetail) {
    Map<SettingVariableTypes, Set<String>> parentIdsByType = new EnumMap<>(SettingVariableTypes.class);
    Map<String, Set<EncryptedDataParent>> parentsByParentId = new HashMap<>();
    populateParentHelperMaps(parentIdsByType, parentsByParentId, parents);
    Set<SecretSetupUsage> rv = new HashSet<>();
    parentIdsByType.forEach((type, parentIds) -> {
      Map<String, Set<EncryptedDataParent>> parentsByParentIdForType =
          Maps.filterKeys(parentsByParentId, Predicates.in(parentIds));
      rv.addAll(buildSecretSetupUsageFromParentsInternal(
          type, encryptionDetail, accountId, secretId, parentsByParentIdForType));
    });
    return rv;
  }

  private Map<String, Set<String>> buildAppEnvMapFromParentsInternal(SettingVariableTypes type,
      @NonNull String accountId, @NonNull String secretId,
      @NonNull Map<String, Set<EncryptedDataParent>> parentsByParentId) {
    Map<String, Set<String>> appEnvMap = new HashMap<>();
    Optional<SecretSetupUsageBuilder> secretSetupUsageBuilderOptional =
        secretSetupUsageBuilderRegistry.getSecretSetupUsageBuilder(type);
    if (!secretSetupUsageBuilderOptional.isPresent()) {
      log.warn("Building setup usages is not supported for {}", type);
      return appEnvMap;
    }
    SecretSetupUsageBuilder secretSetupUsageBuilder = secretSetupUsageBuilderOptional.get();
    return secretSetupUsageBuilder.buildAppEnvMap(accountId, secretId, parentsByParentId);
  }

  private Map<String, Set<String>> buildAppEnvMapFromParents(
      String accountId, String secretId, Set<EncryptedDataParent> parents) {
    Map<SettingVariableTypes, Set<String>> parentIdsByType = new EnumMap<>(SettingVariableTypes.class);
    Map<String, Set<EncryptedDataParent>> parentsByParentId = new HashMap<>();
    populateParentHelperMaps(parentIdsByType, parentsByParentId, parents);
    Map<String, Set<String>> appEnvMap = new HashMap<>();
    parentIdsByType.forEach((type, parentIds) -> {
      Map<String, Set<EncryptedDataParent>> parentsByParentIdForType =
          Maps.filterKeys(parentsByParentId, Predicates.in(parentIds));
      Map<String, Set<String>> subAppEnvMap =
          buildAppEnvMapFromParentsInternal(type, accountId, secretId, parentsByParentIdForType);
      subAppEnvMap.keySet().forEach(appId -> {
        Set<String> subEnvIds = subAppEnvMap.get(appId);
        Set<String> envIds = appEnvMap.computeIfAbsent(appId, k -> new HashSet<>());
        envIds.addAll(subEnvIds);
      });
    });
    return appEnvMap;
  }

  @Override
  public Set<SecretSetupUsage> getSecretUsage(@NonNull String accountId, @NonNull String secretTextId) {
    EncryptedData encryptedData =
        secretsDao.getSecretById(accountId, secretTextId).<InvalidArgumentsException>orElseThrow(() -> {
          throw new InvalidArgumentsException(
              String.format("Could not find secret with id %s", secretTextId), USER_SRE);
        });

    if (isEmpty(encryptedData.getParents())) {
      return Collections.emptySet();
    }

    EncryptionDetail encryptionDetail =
        EncryptionDetail.builder()
            .encryptionType(encryptedData.getEncryptionType())
            .secretManagerName(secretManagerConfigService.getSecretManagerName(encryptedData.getKmsId(), accountId))
            .build();

    return buildSecretSetupUsageFromParents(accountId, secretTextId, encryptedData.getParents(), encryptionDetail);
  }

  @Override
  public Map<String, Set<String>> getUsagesAppEnvMap(String accountId, String secretTextId) {
    EncryptedData encryptedData =
        secretsDao.getSecretById(accountId, secretTextId).<InvalidArgumentsException>orElseThrow(() -> {
          throw new InvalidArgumentsException(
              String.format("Could not find secret with id %s", secretTextId), USER_SRE);
        });

    if (isEmpty(encryptedData.getParents())) {
      return Collections.emptyMap();
    }
    return buildAppEnvMapFromParents(accountId, secretTextId, encryptedData.getParents());
  }
}
