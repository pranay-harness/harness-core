package io.harness.ff;

import static io.harness.beans.FeatureName.NEXT_GEN_ENABLED;
import static io.harness.beans.FeatureName.NG_ACCESS_CONTROL_MIGRATION;
import static io.harness.beans.FeatureName.NG_RBAC_ENABLED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureFlag.FeatureFlagKeys;
import io.harness.beans.FeatureFlag.Scope;
import io.harness.beans.FeatureName;
import io.harness.cf.CFApi;
import io.harness.cf.CfMigrationConfig;
import io.harness.cf.openapi.api.PatchInstruction;
import io.harness.cf.openapi.api.PatchOperation;
import io.harness.cf.openapi.model.Feature;
import io.harness.cf.openapi.model.FeatureState;
import io.harness.cf.openapi.model.Features;
import io.harness.cf.openapi.model.InlineObject;
import io.harness.cf.openapi.model.InlineObject.KindEnum;
import io.harness.cf.openapi.model.ServingRule;
import io.harness.cf.openapi.model.Variation;
import io.harness.configuration.DeployMode;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class FeatureFlagServiceImpl implements FeatureFlagService {
  @Inject private HPersistence persistence;
  @Inject(optional = true)
  @Nullable
  @Named(EventsFrameworkConstants.FEATURE_FLAG_STREAM)
  private Producer eventProducer;

  private final List<FeatureName> featureFlagsToSendEvent =
      Lists.newArrayList(ImmutableList.of(NG_ACCESS_CONTROL_MIGRATION, NG_RBAC_ENABLED, NEXT_GEN_ENABLED));
  private long lastEpoch;
  private final Map<FeatureName, FeatureFlag> cache = new HashMap<>();
  @Inject private CFApi cfApi;
  @Inject private CfMigrationConfig cfMigrationConfig;
  @Override
  public boolean isEnabledReloadCache(FeatureName featureName, String accountId) {
    synchronized (cache) {
      cache.clear();
    }
    return isEnabled(featureName, accountId);
  }

  @Override
  public void enableAccount(FeatureName featureName, String accountId) {
    log.info("Enabling feature name :[{}] for account id: [{}]", featureName.name(), accountId);
    Query<FeatureFlag> query =
        persistence.createQuery(FeatureFlag.class).filter(FeatureFlagKeys.name, featureName.name());
    UpdateOperations<FeatureFlag> updateOperations = persistence.createUpdateOperations(FeatureFlag.class)
                                                         .addToSet(FeatureFlagKeys.accountIds, accountId)
                                                         .setOnInsert(FeatureFlagKeys.name, featureName.name())
                                                         .setOnInsert(FeatureFlagKeys.uuid, generateUuid())
                                                         .setOnInsert(FeatureFlagKeys.obsolete, false)
                                                         .setOnInsert(FeatureFlagKeys.enabled, false);
    FeatureFlag featureFlag = persistence.findAndModify(query, updateOperations, HPersistence.upsertReturnNewOptions);
    if (featureFlagsToSendEvent.contains(featureName)) {
      publishEvent(accountId, featureName, true);
    }
    synchronized (cache) {
      cache.put(featureName, featureFlag);
    }
    log.info("Enabled feature name :[{}] for account id: [{}]", featureName.name(), accountId);
  }

  private void publishEvent(String accountId, FeatureName featureName, boolean enable) {
    try {
      if (eventProducer != null) {
        eventProducer.send(Message.newBuilder()
                               .putAllMetadata(ImmutableMap.of("accountId", accountId))
                               .setData(FeatureFlagChangeDTO.newBuilder()
                                            .setAccountId(accountId)
                                            .setEnable(enable)
                                            .setFeatureName(featureName.toString())
                                            .build()
                                            .toByteString())
                               .build());
      }
    } catch (Exception ex) {
      log.error("Failed to publish account change event for enabling next gen via event framework.", ex);
    }
  }

  @Override
  public FeatureFlag updateFeatureFlagForAccount(String featureName, String accountId, boolean enabled) {
    Optional<FeatureFlag> featureFlagOptional = getFeatureFlag(FeatureName.valueOf(featureName));
    FeatureFlag featureFlag =
        featureFlagOptional.orElseThrow(() -> new InvalidRequestException("Invalid feature flag name: " + featureName));
    if (Objects.isNull(featureFlag.getAccountIds())) {
      featureFlag.setAccountIds(Sets.newHashSet());
    }

    if (enabled) {
      featureFlag.getAccountIds().add(accountId);
    } else {
      featureFlag.getAccountIds().remove(accountId);
    }
    persistence.save(featureFlag);
    if (featureFlagsToSendEvent.contains(FeatureName.valueOf(featureName))) {
      publishEvent(accountId, FeatureName.valueOf(featureName), enabled);
    }
    synchronized (cache) {
      cache.put(FeatureName.valueOf(featureName), featureFlag);
    }
    return featureFlag;
  }

  @Override
  public void enableGlobally(FeatureName featureName) {
    log.info("Enabling feature name :[{}] globally", featureName.name());
    Query<FeatureFlag> query =
        persistence.createQuery(FeatureFlag.class).filter(FeatureFlagKeys.name, featureName.name());
    UpdateOperations<FeatureFlag> updateOperations = persistence.createUpdateOperations(FeatureFlag.class)
                                                         .setOnInsert(FeatureFlagKeys.name, featureName.name())
                                                         .setOnInsert(FeatureFlagKeys.uuid, generateUuid())
                                                         .setOnInsert(FeatureFlagKeys.obsolete, Boolean.FALSE)
                                                         .set(FeatureFlagKeys.enabled, Boolean.TRUE);
    FeatureFlag featureFlag = persistence.findAndModify(query, updateOperations, HPersistence.upsertReturnNewOptions);
    synchronized (cache) {
      cache.put(featureName, featureFlag);
    }
    log.info("Enabled feature name :[{}] globally", featureName.name());
  }

  @Override
  public List<FeatureFlag> getGloballyEnabledFeatureFlags() {
    List<FeatureFlag> globallyEnabledFeatureFlag = new ArrayList<>();

    getAllFeatureFlags().forEach(featureFlag -> {
      if (featureFlag.isEnabled()) {
        globallyEnabledFeatureFlag.add(featureFlag);
      }
    });

    return globallyEnabledFeatureFlag;
  }
  @Override
  public boolean isGlobalEnabled(FeatureName featureName) {
    if (featureName.getScope() != Scope.GLOBAL) {
      log.warn("FeatureFlag {} is not global", featureName.name(), new Exception(""));
    }
    return isEnabled(featureName, null);
  }

  @Override
  public boolean isNotGlobalEnabled(FeatureName featureName) {
    return !isGlobalEnabled(featureName);
  }

  @Override
  public Optional<FeatureFlag> getFeatureFlag(@NonNull FeatureName featureName) {
    FeatureFlag featureFlag;
    synchronized (cache) {
      // if the last access to cache was in different epoch reset it. This will allow for potentially outdated
      // objects to be replaced, and the potential change will be in a relatively same time on all managers.
      long epoch = currentTimeMillis() / Duration.ofMinutes(5).toMillis();
      if (lastEpoch != epoch) {
        lastEpoch = epoch;
        cache.clear();
      }

      featureFlag = cache.computeIfAbsent(featureName,
          key
          -> persistence.createQuery(FeatureFlag.class, excludeAuthority)
                 .filter(FeatureFlagKeys.name, key.name())
                 .get());
    }
    return Optional.ofNullable(featureFlag);
  }

  @Override
  public boolean isEnabled(@NonNull FeatureName featureName, String accountId) {
    Optional<FeatureFlag> featureFlagOptional = getFeatureFlag(featureName);

    if (featureFlagOptional.isPresent()) {
      FeatureFlag featureFlag = featureFlagOptional.get();

      if (featureFlag.isEnabled()) {
        return true;
      }

      if (isEmpty(accountId) && featureName.getScope() == Scope.PER_ACCOUNT) {
        log.error("FeatureFlag isEnabled check without accountId", new Exception(""));
        return false;
      }

      if (isNotEmpty(featureFlag.getAccountIds())) {
        if (featureName.getScope() == Scope.GLOBAL) {
          log.error("A global FeatureFlag isEnabled per specific accounts", new Exception(""));
          return false;
        }
        return featureFlag.getAccountIds().contains(accountId);
      }
    }

    return false;
  }

  @Override
  public boolean isEnabledForAllAccounts(FeatureName featureName) {
    Optional<FeatureFlag> featureFlagOptional = getFeatureFlag(featureName);
    if (featureFlagOptional.isPresent()) {
      FeatureFlag featureFlag = featureFlagOptional.get();

      if (featureFlag.isEnabled()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isNotEnabled(FeatureName featureName, String accountId) {
    return !isEnabled(featureName, accountId);
  }

  @Override
  public Set<String> getAccountIds(@NonNull FeatureName featureName) {
    FeatureFlag featureFlag = getFeatureFlag(featureName).orElse(null);
    if (featureFlag == null || isEmpty(featureFlag.getAccountIds())) {
      return new HashSet<>();
    }
    if (featureName.getScope() == Scope.GLOBAL) {
      log.warn("FeatureFlag {} is global, should not have accountIds", featureName.name(), new Exception(""));
    }
    return featureFlag.getAccountIds();
  }

  @Override
  public void initializeFeatureFlags(DeployMode deployMode, String featureNames) {
    Set<String> definedNames = Arrays.stream(FeatureName.values()).map(FeatureName::name).collect(toSet());

    // Mark persisted flags that are no longer defined as obsolete
    persistence.update(persistence.createQuery(FeatureFlag.class, excludeAuthority)
                           .filter(FeatureFlagKeys.obsolete, false)
                           .field(FeatureFlagKeys.name)
                           .notIn(definedNames),
        persistence.createUpdateOperations(FeatureFlag.class).set(FeatureFlagKeys.obsolete, true));

    // Mark persisted flags that are defined as not obsolete
    persistence.update(persistence.createQuery(FeatureFlag.class, excludeAuthority)
                           .filter(FeatureFlagKeys.obsolete, true)
                           .field(FeatureFlagKeys.name)
                           .in(definedNames),
        persistence.createUpdateOperations(FeatureFlag.class).set(FeatureFlagKeys.obsolete, false));

    // Delete flags that were marked obsolete more than ten days ago
    persistence.delete(persistence.createQuery(FeatureFlag.class, excludeAuthority)
                           .filter(FeatureFlagKeys.obsolete, true)
                           .field(FeatureFlagKeys.lastUpdatedAt)
                           .lessThan(currentTimeMillis() - TimeUnit.DAYS.toMillis(10)));

    // Persist new flags initialized as enabled false
    Set<String> persistedNames = persistence.createQuery(FeatureFlag.class, excludeAuthority)
                                     .project(FeatureFlagKeys.name, true)
                                     .asList()
                                     .stream()
                                     .map(FeatureFlag::getName)
                                     .collect(toSet());
    List<FeatureFlag> newFeatureFlags = definedNames.stream()
                                            .filter(name -> !persistedNames.contains(name))
                                            .map(name -> FeatureFlag.builder().name(name).enabled(false).build())
                                            .collect(toList());
    persistence.save(newFeatureFlags);

    // For on-prem, set all enabled values from the list of enabled flags in the configuration
    if (DeployMode.isOnPrem(deployMode.name())) {
      List<String> enabled = isBlank(featureNames)
          ? emptyList()
          : Splitter.on(',').omitEmptyStrings().trimResults().splitToList(featureNames);
      for (String name : definedNames) {
        persistence.update(
            persistence.createQuery(FeatureFlag.class, excludeAuthority).filter(FeatureFlagKeys.name, name),
            persistence.createUpdateOperations(FeatureFlag.class).set(FeatureFlagKeys.enabled, enabled.contains(name)));
      }
    }
    syncWithCFServer();
  }

  private void syncWithCFServer() {
    if (!cfMigrationConfig.isEnabled()) {
      log.debug("Not initializing with CF Server since CF Migration is disabled");
      return;
    }
    try {
      String cfAccount = cfMigrationConfig.getAccount();
      String cfOrg = cfMigrationConfig.getOrg();
      String cfProject = cfMigrationConfig.getProject();
      String cfEnvironment = cfMigrationConfig.getEnvironment();
      Set<String> cfFeatures = new HashSet<>();
      Features features = cfApi.getAllFeatures(cfAccount, cfOrg, cfProject, cfEnvironment, 0, Integer.MAX_VALUE);
      Map<String, Feature> featureMap = new HashMap<>();
      for (Feature feature : features.getFeatures()) {
        log.info("Feature Name " + feature.getName());
        featureMap.put(feature.getName(), feature);
        cfFeatures.add(feature.getName());
      }

      /**
       * For every feature flag in Harness, do the following
       */
      for (FeatureName featureName : FeatureName.values()) {
        Optional<FeatureFlag> featureFlag = getFeatureFlag(featureName);
        /**
         * If featureFlag is not present in db, something is wrong on the Harness side. We will not sync this
         * featureFlag in CF
         */
        if (!featureFlag.isPresent()) {
          log.warn("CF-SYNC FeatureFlag [{}] is not present in Harness DB, skipping it ", featureName.name());
          continue;
        }
        /**
         * If Harness CF does not contain this featureFlag, go ahead and create it
         */
        if (!cfFeatures.contains(featureName.name())) {
          try {
            cfApi.createFeatureFlag(cfAccount, cfOrg, createCFFeatureFlag(featureName.name(), featureName.getScope()));
            Feature feature = cfApi.getFeatureFlag(featureName.name(), cfAccount, cfOrg, cfProject, cfEnvironment);
            featureMap.put(featureName.name(), feature);
            log.info("CF-SYNC Created featureFlag [{}] in CF", featureName);
          } catch (Exception e) {
            log.error("CF-SYNC Failed to create featureFlag in CF", e);
            continue;
          }
        } else {
          log.info("CF-SYNC FeatureFlag [{}] already present in CF, not creating it again", featureName.name());
        }

        List<PatchInstruction> instructions = new ArrayList<>();

        Feature cfFeature = featureMap.get(featureName.name());
        /**
         * If for the given environment, this featureFlag has state off, go ahead and turn it on
         * Turn featureFlag on if it is off
         */
        if (cfFeature.getEnvProperties().getState().getValue().equals(FeatureState.OFF.getValue())) {
          /*
           * Turn featureFlag on
           * */
          log.info("CF-SYNC FeatureFlag [{}] in CF has state OFF, turning it on", featureName.name());
          PatchInstruction turnOnPatchInstruction = cfApi.getFeatureFlagOnPatchInstruction(true);
          instructions.add(turnOnPatchInstruction);
        } else {
          log.info("CF-SYNC FeatureFlag [{}] in CF has state ON", featureName.name());
        }

        /**
         * If the featureFlag is enabled on Harness side, then we should turn the defaultServe to on
         * This is based on the logic in isEnabled
         *
         * If featureFlag is enabled, then
         * turn defaultOnVariation to true if it is set to false
         *
         */
        final String defaultServeVariation = cfFeature.getEnvProperties().getDefaultServe().getVariation();
        if (featureFlag.get().isEnabled()) {
          if (defaultServeVariation.equals("false")) {
            PatchInstruction patchInstruction = cfApi.getFeatureFlagDefaultServePatchInstruction(true);
            instructions.add(patchInstruction);
            log.info("CF-SYNC CF FeatureFlag [{}] is has defaultServe OFF, turning it ON", featureName.name());
          } else {
            log.info("CF-SYNC CF FeatureFlag [{}] is has defaultServe ON, not turning it ON again", featureName.name());
          }
        } else {
          if (defaultServeVariation.equals("true")) {
            PatchInstruction patchInstruction = cfApi.getFeatureFlagDefaultServePatchInstruction(false);
            instructions.add(patchInstruction);
            log.info("CF-SYNC CF FeatureFlag [{}] is has defaultServe ON, turning it OFF", featureName.name());
          }
        }
        /**
         * If featureFlag list of accounts is present, add custom rule for each accountID and serveVariation to
         * true
         */
        Set<String> accountIds = featureFlag.get().getAccountIds();
        Set<String> rulesTobeDeleted = new HashSet<>();
        Set<String> tobeAddedAccountIds = new HashSet<>();
        final List<ServingRule> rules = cfFeature.getEnvProperties().getRules();
        int maxPriority = 0;
        if (accountIds != null && accountIds.size() > 0) {
          tobeAddedAccountIds.addAll(accountIds);
          if (rules != null && rules.size() > 0) {
            for (ServingRule rule : rules) {
              if (rule.getClauses().size() > 0) {
                final String accountId = rule.getClauses().get(0).getValues().get(0);
                if (tobeAddedAccountIds.contains(accountId)) {
                  tobeAddedAccountIds.remove(accountId);
                } else {
                  rulesTobeDeleted.add(rule.getRuleId());
                }
              } else {
                log.warn(
                    "CF-SYNC Invalid rule [{}] for FF[{}], environment [{}] , does not contain any clause, marking for deletion",
                    rule.getRuleId(), featureName.name(), cfEnvironment);
                rulesTobeDeleted.add(rule.getRuleId());
              }

              maxPriority = Math.max(maxPriority, rule.getPriority());
            }
          }

        } else {
          /**
           * If there are no accounts set for this rule, delete all rules for the featureFlag
           */
          if (rules.size() > 0) {
            rules.forEach(rule -> rulesTobeDeleted.add(rule.getRuleId()));
          }
        }

        if (tobeAddedAccountIds.size() > 0) {
          log.info(
              "CF-SYNC Creating Rules for AccountIDs for FF[{}], environment [{}]", featureName.name(), cfEnvironment);
          List<PatchInstruction> accountToBeAddedPatchInstruction =
              cfApi.getFeatureFlagRulesForTargetingAccounts(tobeAddedAccountIds, maxPriority + 100);
          instructions.addAll(accountToBeAddedPatchInstruction);
        }

        if (rulesTobeDeleted.size() > 0) {
          for (String rule : rulesTobeDeleted) {
            log.info("CF-SYNC Deleting unwanted rule for FF[{}], environment [{}], ruleID : [{}]", featureName.name(),
                cfEnvironment, rule);
            instructions.add(cfApi.removeRule(rule));
          }
        }

        if (instructions.size() > 0) {
          PatchOperation patchOperation = PatchOperation.builder().instructions(instructions).build();
          cfFeature =
              cfApi.patchFeature(featureName.name(), cfAccount, cfOrg, cfProject, cfEnvironment, patchOperation);
        }
      }
    } catch (Exception e) {
      log.error("Failed to sync with Harness CF", e);
    }
  }

  @NotNull
  private InlineObject createCFFeatureFlag(String featureName, Scope scope) {
    InlineObject inlineObject = new InlineObject();
    inlineObject.setProject("project1");
    inlineObject.setIdentifier(featureName);
    inlineObject.setName(featureName);
    inlineObject.setKind(KindEnum.BOOLEAN);
    Variation trueVariation = new Variation();
    trueVariation.setIdentifier("true");
    trueVariation.setName("true");
    trueVariation.setValue("true");

    Variation falseVariation = new Variation();
    falseVariation.setIdentifier("false");
    falseVariation.setName("false");
    falseVariation.setValue("false");

    List<Variation> variations = new ArrayList<>();
    variations.add(trueVariation);
    variations.add(falseVariation);

    inlineObject.setVariations(variations);
    inlineObject.setDefaultOffVariation(falseVariation.getIdentifier());
    inlineObject.setDefaultOnVariation(falseVariation.getIdentifier());

    return inlineObject;
  }

  /**
   * Used to return list of feature flags to admin tool
   * @return List of all feature flags defined
   */
  @Override
  public List<FeatureFlag> getAllFeatureFlags() {
    return persistence.createQuery(FeatureFlag.class).asList();
  }

  /**
   * used by admin tool to batch add/remove accounts in feature flag and enable/disable feature flag globally
   * @param featureFlagName name
   * @param featureFlag feature flag
   * @return updated feature flag
   */
  @Override
  public Optional<FeatureFlag> updateFeatureFlag(String featureFlagName, FeatureFlag featureFlag) {
    Optional<FeatureFlag> featureFlagOptional = getFeatureFlag(FeatureName.valueOf(featureFlagName));
    if (!featureFlagOptional.isPresent()) {
      return Optional.empty();
    }
    persistence.save(featureFlag);
    if (featureFlagsToSendEvent.contains(FeatureName.valueOf(featureFlagName))) {
      FeatureFlag existingFeatureFlag = featureFlagOptional.get();
      Set<String> existingAccounts =
          existingFeatureFlag.getAccountIds() != null ? existingFeatureFlag.getAccountIds() : emptySet();
      Set<String> newAccounts = featureFlag.getAccountIds() != null ? featureFlag.getAccountIds() : emptySet();
      Set<String> accountsAdded = Sets.difference(newAccounts, existingAccounts);
      Set<String> accountsRemoved = Sets.difference(existingAccounts, newAccounts);

      // for accounts which have been added, send enabled event
      accountsAdded.forEach(account -> publishEvent(account, FeatureName.valueOf(featureFlagName), true));

      // for accounts which have been removed, send disabled event
      accountsRemoved.forEach(account -> publishEvent(account, FeatureName.valueOf(featureFlagName), false));
    }
    synchronized (cache) {
      cache.put(FeatureName.valueOf(featureFlagName), featureFlag);
    }
    return Optional.of(featureFlag);
  }

  /**
   * Removes an account from the FeatureFlags collection
   * @param accountId
   */
  @Override
  public void removeAccountReferenceFromAllFeatureFlags(String accountId) {
    List<FeatureFlag> featureFlags = getAllFeatureFlags();
    for (FeatureFlag featureFlag : featureFlags) {
      try {
        updateFeatureFlagForAccount(featureFlag.getName(), accountId, false);
      } catch (Exception e) {
        log.error(
            "Exception occurred while deleting account {} from FeatureFlag {}", accountId, featureFlag.getName(), e);
      }
    }
  }
}
