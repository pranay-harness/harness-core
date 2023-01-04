/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngsettings.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.PersistenceUtils.DEFAULT_RETRY_POLICY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.exception.EntityNotFoundException;
import io.harness.exception.InvalidRequestException;
import io.harness.licensing.Edition;
import io.harness.licensing.services.LicenseService;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingUpdateType;
import io.harness.ngsettings.dto.SettingDTO;
import io.harness.ngsettings.dto.SettingRequestDTO;
import io.harness.ngsettings.dto.SettingResponseDTO;
import io.harness.ngsettings.dto.SettingUpdateResponseDTO;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.entities.Setting;
import io.harness.ngsettings.entities.Setting.SettingKeys;
import io.harness.ngsettings.entities.SettingConfiguration;
import io.harness.ngsettings.entities.SettingConfiguration.SettingConfigurationKeys;
import io.harness.ngsettings.events.SettingRestoreEvent;
import io.harness.ngsettings.events.SettingUpdateEvent;
import io.harness.ngsettings.mapper.SettingsMapper;
import io.harness.ngsettings.services.SettingValidator;
import io.harness.ngsettings.services.SettingsService;
import io.harness.ngsettings.utils.SettingUtils;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.ngsettings.spring.SettingConfigurationRepository;
import io.harness.repositories.ngsettings.spring.SettingRepository;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
@Slf4j
public class SettingsServiceImpl implements SettingsService {
  private final SettingConfigurationRepository settingConfigurationRepository;
  private final SettingRepository settingRepository;
  private final SettingsMapper settingsMapper;
  private final TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;
  private final Map<String, SettingValidator> settingValidatorMap;
  private final LicenseService licenseService;

  @Inject
  public SettingsServiceImpl(SettingConfigurationRepository settingConfigurationRepository,
      SettingRepository settingRepository, SettingsMapper settingsMapper,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, OutboxService outboxService,
      Map<String, SettingValidator> settingValidatorMap, LicenseService licenseService) {
    this.settingConfigurationRepository = settingConfigurationRepository;
    this.settingRepository = settingRepository;
    this.settingsMapper = settingsMapper;
    this.transactionTemplate = transactionTemplate;
    this.outboxService = outboxService;
    this.settingValidatorMap = settingValidatorMap;
    this.licenseService = licenseService;
  }

  @Override
  public List<SettingResponseDTO> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SettingCategory category, String groupIdentifier) {
    Edition accountEdition = getEditionForAccount(accountIdentifier);
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
    Map<String, SettingConfiguration> settingConfigurations = getSettingConfigurations(
        accountIdentifier, orgIdentifier, projectIdentifier, category, groupIdentifier, accountEdition);
    Map<Pair<String, Scope>, Setting> settings =
        getSettings(accountIdentifier, orgIdentifier, projectIdentifier, category, groupIdentifier);
    List<SettingResponseDTO> settingResponseDTOList = new ArrayList<>();
    settingConfigurations.forEach((identifier, settingConfiguration) -> {
      Pair<String, Scope> currentScopeSettingKey = new ImmutablePair<>(identifier, scope);
      Setting parentSetting = getSettingFromParentScope(scope, identifier, settingConfiguration, accountEdition);
      if (settings.containsKey(currentScopeSettingKey)) {
        settingResponseDTOList.add(settingsMapper.writeSettingResponseDTO(
            settings.get(currentScopeSettingKey), settingConfiguration, true, parentSetting.getValue()));
      } else {
        boolean isSettingEditable =
            SettingUtils.isSettingEditableForAccountEdition(accountEdition, settingConfiguration)
            && (ScopeLevel.of(accountIdentifier, orgIdentifier, projectIdentifier) == ScopeLevel.ACCOUNT
                || parentSetting.getAllowOverrides());
        settingResponseDTOList.add(
            settingsMapper.writeSettingResponseDTO(parentSetting, settingConfiguration, isSettingEditable));
      }
    });
    return settingResponseDTOList;
  }

  @Override
  public List<SettingUpdateResponseDTO> update(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      List<SettingRequestDTO> settingRequestDTOList) {
    List<SettingUpdateResponseDTO> settingResponses = new ArrayList<>();
    Scope currentScope = Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
    settingRequestDTOList.forEach(settingRequestDTO -> {
      try {
        SettingResponseDTO settingResponseDTO;
        checkOverridesAreAllowedInParentScope(currentScope, settingRequestDTO);
        if (settingRequestDTO.getUpdateType() == SettingUpdateType.RESTORE) {
          settingResponseDTO = restoreSetting(accountIdentifier, orgIdentifier, projectIdentifier, settingRequestDTO);
        } else {
          settingResponseDTO = updateSetting(accountIdentifier, orgIdentifier, projectIdentifier, settingRequestDTO);
        }
        settingResponses.add(settingsMapper.writeBatchResponseDTO(settingResponseDTO));
      } catch (Exception exception) {
        log.error("Error when updating setting:", exception);
        settingResponses.add(settingsMapper.writeBatchResponseDTO(settingRequestDTO.getIdentifier(), exception));
      }
    });
    return settingResponses;
  }

  private void checkOverridesAreAllowedInParentScope(Scope currentScope, SettingRequestDTO settingRequestDTO) {
    ScopeLevel currentScopeLevel = ScopeLevel.of(currentScope);
    if (currentScopeLevel.equals(ScopeLevel.ACCOUNT)) {
      return;
    }
    while ((currentScope = SettingUtils.getParentScope(currentScope)) != null) {
      Optional<Setting> setting =
          settingRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
              currentScope.getAccountIdentifier(), currentScope.getOrgIdentifier(), currentScope.getProjectIdentifier(),
              settingRequestDTO.getIdentifier());
      if (!setting.isPresent()) {
        continue;
      }
      if (Boolean.FALSE.equals(setting.get().getAllowOverrides())) {
        throw new InvalidRequestException(
            String.format("Setting- %s cannot be overridden at the current scope", settingRequestDTO.getIdentifier()));
      } else {
        return;
      }
    }
    Optional<SettingConfiguration> settingConfiguration =
        settingConfigurationRepository.findByIdentifier(settingRequestDTO.getIdentifier());
    if (settingConfiguration.isEmpty()) {
      throw new InvalidRequestException(String.format("Setting- %s does not exist", settingRequestDTO.getIdentifier()));
    }
    if (SettingUtils.getHighestScopeForSetting(settingConfiguration.get().getAllowedScopes())
            .equals(currentScopeLevel)) {
      return;
    }
    if (Boolean.FALSE.equals(settingConfiguration.get().getAllowOverrides())) {
      throw new InvalidRequestException(
          String.format("Setting- %s cannot be overridden at the current scope", settingRequestDTO.getIdentifier()));
    }
  }

  @Override
  public SettingValueResponseDTO get(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Edition accountEdition = getEditionForAccount(accountIdentifier);
    SettingConfiguration settingConfiguration =
        getSettingConfiguration(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    Optional<Setting> existingSetting =
        settingRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    String value;
    if (existingSetting.isPresent()) {
      value = existingSetting.get().getValue();
    } else {
      value = getSettingFromParentScope(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), identifier,
          settingConfiguration, accountEdition)
                  .getValue();
    }
    return SettingValueResponseDTO.builder().valueType(settingConfiguration.getValueType()).value(value).build();
  }

  private Setting getSettingFromParentScope(
      Scope currentScope, String identifier, SettingConfiguration settingConfiguration, String defaultValue) {
    while ((currentScope = SettingUtils.getParentScope(currentScope)) != null) {
      Optional<Setting> setting =
          settingRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
              currentScope.getAccountIdentifier(), currentScope.getOrgIdentifier(), currentScope.getProjectIdentifier(),
              identifier);
      if (setting.isPresent()) {
        return setting.get();
      }
    }
    return settingsMapper.toSetting(null, settingsMapper.writeSettingDTO(settingConfiguration, true, defaultValue));
  }

  private Setting getSettingFromParentScope(
      Scope currentScope, String identifier, SettingConfiguration settingConfiguration, Edition accountEdition) {
    String defaultValue = SettingUtils.getDefaultValue(accountEdition, settingConfiguration);
    return getSettingFromParentScope(currentScope, identifier, settingConfiguration, defaultValue);
  }

  @Override
  public List<SettingConfiguration> listDefaultSettings() {
    List<SettingConfiguration> settingConfigurationList = new ArrayList<>();
    for (SettingConfiguration settingConfiguration : settingConfigurationRepository.findAll()) {
      settingConfigurationList.add(settingConfiguration);
    }
    return settingConfigurationList;
  }

  @Override
  public void removeSetting(String identifier) {
    Optional<SettingConfiguration> exisingSettingConfig = settingConfigurationRepository.findByIdentifier(identifier);
    exisingSettingConfig.ifPresent(settingConfigurationRepository::delete);
    List<Setting> existingSettings = settingRepository.findByIdentifier(identifier);
    settingRepository.deleteAll(existingSettings);
  }

  @Override
  public SettingConfiguration upsertSettingConfiguration(SettingConfiguration settingConfiguration) {
    SettingUtils.validate(settingConfiguration);
    return settingConfigurationRepository.save(settingConfiguration);
  }

  @Override
  public void deleteByScopeLevel(ScopeLevel scopeLevel, String identifier) {
    Criteria criteria = Criteria.where(SettingKeys.identifier).is(identifier);
    switch (scopeLevel) {
      case ACCOUNT:
        criteria.and(SettingKeys.orgIdentifier).is(null).and(SettingKeys.projectIdentifier).is(null);
        break;
      case ORGANIZATION:
        criteria.and(SettingKeys.orgIdentifier).ne(null).and(SettingKeys.projectIdentifier).is(null);
        break;
      case PROJECT:
        criteria.and(SettingKeys.orgIdentifier).ne(null).and(SettingKeys.projectIdentifier).ne(null);
        break;
      default:
        throw new InvalidRequestException(
            String.format("Invalid scope- %s present in the settings.yml", scopeLevel.toString()));
    }
    settingRepository.delete(criteria);
  }

  private Map<Pair<String, Scope>, Setting> getSettings(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, SettingCategory category, String groupIdentifier) {
    List<Setting> settings;
    Criteria criteria =
        Criteria.where(SettingKeys.accountIdentifier)
            .is(accountIdentifier)
            .and(SettingKeys.category)
            .is(category)
            .andOperator(new Criteria().orOperator(
                Criteria.where(SettingKeys.orgIdentifier).is(null).and(SettingKeys.projectIdentifier).is(null),
                Criteria.where(SettingKeys.orgIdentifier).is(orgIdentifier).and(SettingKeys.projectIdentifier).is(null),
                Criteria.where(SettingKeys.orgIdentifier)
                    .is(orgIdentifier)
                    .and(SettingKeys.projectIdentifier)
                    .is(projectIdentifier)));
    if (isNotEmpty(groupIdentifier)) {
      criteria.and(SettingKeys.groupIdentifier).is(groupIdentifier);
    }
    settings = settingRepository.findAll(criteria);
    return settings.stream().collect(Collectors.toMap(setting
        -> new ImmutablePair<>(setting.getIdentifier(),
            Scope.of(accountIdentifier, setting.getOrgIdentifier(), setting.getProjectIdentifier())),
        Function.identity()));
  }

  private Map<String, SettingConfiguration> getSettingConfigurations(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, SettingCategory category, String groupIdentifier, Edition accountEdition) {
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
    List<ScopeLevel> scopes = Collections.singletonList(ScopeLevel.of(scope));

    List<SettingConfiguration> defaultSettingConfigurations;
    Criteria criteria =
        Criteria.where(SettingConfigurationKeys.category)
            .is(category)
            .and(SettingConfigurationKeys.allowedScopes)
            .in(scopes)
            .orOperator(Criteria.where(SettingConfigurationKeys.allowedPlans).is(null),
                Criteria.where(SettingConfigurationKeys.allowedPlans + "." + accountEdition.toString()).exists(true));
    if (isNotEmpty(groupIdentifier)) {
      criteria.and(SettingConfigurationKeys.groupIdentifier).is(groupIdentifier);
    }

    defaultSettingConfigurations = settingConfigurationRepository.findAll(criteria);

    return defaultSettingConfigurations.stream().collect(
        Collectors.toMap(SettingConfiguration::getIdentifier, Function.identity()));
  }

  private SettingResponseDTO updateSetting(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, SettingRequestDTO settingRequestDTO) {
    Edition accountEdition = getEditionForAccount(accountIdentifier);
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
    SettingConfiguration settingConfiguration =
        getSettingConfiguration(accountIdentifier, orgIdentifier, projectIdentifier, settingRequestDTO.getIdentifier());

    checkIfAccountPlanAllowsSettingEdit(accountEdition, settingConfiguration);

    Optional<Setting> settingOptional =
        settingRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, settingRequestDTO.getIdentifier());
    SettingDTO newSettingDTO;
    SettingDTO oldSettingDTO;
    String defaultValue = SettingUtils.getDefaultValue(accountEdition, settingConfiguration);
    if (settingOptional.isPresent()) {
      oldSettingDTO = settingsMapper.writeSettingDTO(settingOptional.get(), settingConfiguration, true, defaultValue);
      newSettingDTO = settingsMapper.writeNewDTO(
          settingOptional.get(), settingRequestDTO, settingConfiguration, true, defaultValue);
    } else {
      oldSettingDTO = settingsMapper.writeSettingDTO(settingConfiguration, true, defaultValue);
      newSettingDTO = settingsMapper.writeNewDTO(
          orgIdentifier, projectIdentifier, settingRequestDTO, settingConfiguration, true, defaultValue);
    }
    if (Boolean.FALSE.equals(settingRequestDTO.getAllowOverrides())) {
      deleteSettingInSubScopes(scope, settingRequestDTO);
    }
    SettingUtils.validate(newSettingDTO);
    customValidation(accountIdentifier, oldSettingDTO, newSettingDTO);

    return Failsafe.with(DEFAULT_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      Setting setting = settingRepository.upsert(settingsMapper.toSetting(accountIdentifier, newSettingDTO));
      Setting parentSetting =
          getSettingFromParentScope(scope, settingRequestDTO.getIdentifier(), settingConfiguration, defaultValue);
      outboxService.save(new SettingUpdateEvent(accountIdentifier, oldSettingDTO, newSettingDTO));
      return settingsMapper.writeSettingResponseDTO(setting, settingConfiguration, true, parentSetting.getValue());
    }));
  }

  private SettingConfiguration getSettingConfiguration(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
    Optional<SettingConfiguration> settingConfigurationOptional =
        settingConfigurationRepository.findByIdentifierAndAllowedScopesIn(
            identifier, Collections.singletonList(ScopeLevel.of(scope)));
    if (settingConfigurationOptional.isEmpty()) {
      throw new EntityNotFoundException(String.format(
          "Setting [%s] is either invalid or is not applicable in scope [%s]", identifier, ScopeLevel.of(scope)));
    }
    return settingConfigurationOptional.get();
  }

  private SettingResponseDTO restoreSetting(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, SettingRequestDTO settingRequestDTO) {
    Edition accountEdition = getEditionForAccount(accountIdentifier);
    SettingConfiguration settingConfiguration =
        getSettingConfiguration(accountIdentifier, orgIdentifier, projectIdentifier, settingRequestDTO.getIdentifier());

    checkIfAccountPlanAllowsSettingEdit(accountEdition, settingConfiguration);

    Optional<Setting> setting =
        settingRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, settingRequestDTO.getIdentifier());
    SettingDTO settingDTO;
    SettingDTO oldSettingDTO;
    String defaultValue = SettingUtils.getDefaultValue(accountEdition, settingConfiguration);
    if (setting.isPresent()) {
      oldSettingDTO = settingsMapper.writeSettingDTO(setting.get(), settingConfiguration, true, defaultValue);
      settingDTO =
          settingsMapper.writeNewDTO(setting.get(), settingRequestDTO, settingConfiguration, true, defaultValue);
    } else {
      oldSettingDTO = settingsMapper.writeSettingDTO(settingConfiguration, true, defaultValue);
      settingDTO = settingsMapper.writeNewDTO(
          orgIdentifier, projectIdentifier, settingRequestDTO, settingConfiguration, true, defaultValue);
    }
    customValidation(accountIdentifier, oldSettingDTO, settingDTO);
    return Failsafe.with(DEFAULT_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      setting.ifPresent(settingRepository::delete);
      Setting parentSetting = getSettingFromParentScope(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier),
          settingRequestDTO.getIdentifier(), settingConfiguration, defaultValue);
      outboxService.save(new SettingRestoreEvent(accountIdentifier, oldSettingDTO, settingDTO));
      return settingsMapper.writeSettingResponseDTO(parentSetting, settingConfiguration, true);
    }));
  }

  private void customValidation(String accountIdentifier, SettingDTO oldSettingDTO, SettingDTO newSettingDTO) {
    SettingValidator settingValidator = settingValidatorMap.get(oldSettingDTO.getIdentifier());
    if (settingValidator != null) {
      settingValidator.validate(accountIdentifier, oldSettingDTO, newSettingDTO);
    }
  }

  private void deleteSettingInSubScopes(Scope currentScope, SettingRequestDTO settingRequestDTO) {
    ScopeLevel currentScopeLevel = ScopeLevel.of(currentScope);
    if (currentScopeLevel.equals(ScopeLevel.ACCOUNT)) {
      settingRepository.deleteByAccountIdentifierAndOrgIdentifierNotNullAndIdentifier(
          currentScope.getAccountIdentifier(), settingRequestDTO.getIdentifier());
    } else if (currentScopeLevel.equals(ScopeLevel.ORGANIZATION)) {
      settingRepository.deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifierNotNullAndIdentifier(
          currentScope.getAccountIdentifier(), currentScope.getOrgIdentifier(), settingRequestDTO.getIdentifier());
    }
  }

  private Edition getEditionForAccount(String accountIdentifier) {
    return licenseService.calculateAccountEdition(accountIdentifier);
  }

  private void checkIfAccountPlanAllowsSettingEdit(Edition edition, SettingConfiguration settingConfiguration) {
    if (!SettingUtils.isSettingEditableForAccountEdition(edition, settingConfiguration)) {
      throw new InvalidRequestException(String.format(
          "Your current account plan does not support editing the setting- %s", settingConfiguration.getIdentifier()));
    }
  }
}
