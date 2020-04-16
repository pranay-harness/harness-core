package software.wings.security.encryption.setupusage;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER_SRE;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidArgumentsException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.EncryptionDetail;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Singleton
@Slf4j
public class SecretSetupUsageServiceImpl implements SecretSetupUsageService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private SecretSetupUsageBuilderRegistry secretSetupUsageBuilderRegistry;

  @Override
  public Set<SecretSetupUsage> getSecretUsage(@NonNull String accountId, @NonNull String secretTextId) {
    EncryptedData encryptedData = Optional.ofNullable(wingsPersistence.get(EncryptedData.class, secretTextId))
                                      .<InvalidArgumentsException>orElseThrow(() -> {
                                        throw new InvalidArgumentsException(
                                            String.format("Could not find secret with id %s", secretTextId), USER_SRE);
                                      });

    if (isEmpty(encryptedData.getParents())) {
      return Collections.emptySet();
    }

    EncryptionDetail encryptionDetail = EncryptionDetail.builder()
                                            .encryptionType(encryptedData.getEncryptionType())
                                            .secretManagerName(secretManagerConfigService.getSecretManagerName(
                                                encryptedData.getKmsId(), encryptedData.getEncryptionType(), accountId))
                                            .build();

    return buildSecretSetupUsageFromParents(accountId, secretTextId, encryptedData.getParents(), encryptionDetail);
  }

  private Set<SecretSetupUsage> buildSecretSetupUsageFromParents(@NonNull String accountId, @NonNull String secretId,
      @NonNull Set<EncryptedDataParent> parents, @NonNull EncryptionDetail encryptionDetail) {
    Map<SettingVariableTypes, Set<String>> parentIdsByType = new EnumMap<>(SettingVariableTypes.class);
    Map<String, Set<EncryptedDataParent>> parentsByParentId = new HashMap<>();
    parents.forEach(parent -> {
      Set<String> parentIdsTypeSet = parentIdsByType.computeIfAbsent(parent.getType(), k -> new HashSet<>());
      Set<EncryptedDataParent> parentsIdSet = parentsByParentId.computeIfAbsent(parent.getId(), k -> new HashSet<>());
      parentIdsTypeSet.add(parent.getId());
      parentsIdSet.add(parent);
    });

    Set<SecretSetupUsage> rv = new HashSet<>();
    parentIdsByType.forEach((type, parentIds) -> {
      Map<String, Set<EncryptedDataParent>> parentsByParentIdForType =
          Maps.filterKeys(parentsByParentId, Predicates.in(parentIds));
      rv.addAll(buildSecretSetupUsageFromParentsInternal(
          type, encryptionDetail, accountId, secretId, parentsByParentIdForType));
    });
    return rv;
  }

  private Set<SecretSetupUsage> buildSecretSetupUsageFromParentsInternal(@NonNull SettingVariableTypes type,
      @NonNull EncryptionDetail encryptionDetail, @NonNull String accountId, @NonNull String secretId,
      @NonNull Map<String, Set<EncryptedDataParent>> parentsByParentId) {
    Set<SecretSetupUsage> secretSetupUsages = new HashSet<>();
    Optional<SecretSetupUsageBuilder> secretSetupUsageBuilderOptional =
        secretSetupUsageBuilderRegistry.getSecretSetupUsageBuilder(type);
    if (!secretSetupUsageBuilderOptional.isPresent()) {
      logger.warn("Building setup usages is not supported for {}", type);
      return secretSetupUsages;
    }
    SecretSetupUsageBuilder secretSetupUsageBuilder = secretSetupUsageBuilderOptional.get();
    return secretSetupUsageBuilder.buildSecretSetupUsages(accountId, secretId, parentsByParentId, encryptionDetail);
  }
}