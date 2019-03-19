package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeValidate;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.atteo.evo.inflector.English.plural;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.ENV_ID_KEY;
import static software.wings.beans.SettingAttribute.NAME_KEY;
import static software.wings.beans.SettingAttribute.VALUE_TYPE_KEY;
import static software.wings.beans.StringValue.Builder.aStringValue;
import static software.wings.common.Constants.BACKUP_PATH;
import static software.wings.common.Constants.DEFAULT_BACKUP_PATH;
import static software.wings.common.Constants.DEFAULT_RUNTIME_PATH;
import static software.wings.common.Constants.DEFAULT_STAGING_PATH;
import static software.wings.common.Constants.DEFAULT_WINDOWS_RUNTIME_PATH;
import static software.wings.common.Constants.RUNTIME_PATH;
import static software.wings.common.Constants.STAGING_PATH;
import static software.wings.common.Constants.WINDOWS_RUNTIME_PATH;
import static software.wings.service.intfc.security.SecretManager.ENCRYPTED_FIELD_MASK;
import static software.wings.utils.UsageRestrictionsUtil.getAllAppAllEnvUsageRestrictions;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.observer.Rejection;
import io.harness.observer.Subject;
import io.harness.persistence.HIterator;
import io.harness.validation.Create;
import lombok.Getter;
import org.mongodb.morphia.annotations.Transient;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Event.Type;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.StringValue;
import software.wings.beans.ValidationResult;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.manipulation.SettingsServiceManipulationObserver;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.RestrictionsAndAppEnvMap;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.utils.CacheHelper;
import software.wings.utils.CryptoUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 5/17/16.
 */
@ValidateOnExecution
@Singleton
public class SettingsServiceImpl implements SettingsService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingValidationService settingValidationService;
  @Inject private AppService appService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Transient @Inject private SecretManager secretManager;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private YamlPushService yamlPushService;
  @Inject private GitConfigHelperService gitConfigHelperService;

  @Getter private Subject<SettingsServiceManipulationObserver> manipulationSubject = new Subject<>();
  @Inject private CacheHelper cacheHelper;
  @Inject private EnvironmentService envService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<SettingAttribute> list(
      PageRequest<SettingAttribute> req, String appIdFromRequest, String envIdFromRequest) {
    try {
      PageResponse<SettingAttribute> pageResponse = wingsPersistence.query(SettingAttribute.class, req);

      List<SettingAttribute> filteredSettingAttributes =
          getFilteredSettingAttributes(pageResponse.getResponse(), appIdFromRequest, envIdFromRequest);

      return aPageResponse()
          .withResponse(filteredSettingAttributes)
          .withTotal(filteredSettingAttributes.size())
          .build();

    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private List<SettingAttribute> getFilteredSettingAttributes(
      List<SettingAttribute> inputSettingAttributes, String appIdFromRequest, String envIdFromRequest) {
    if (inputSettingAttributes == null) {
      return Collections.emptyList();
    }

    if (inputSettingAttributes.size() == 0) {
      return inputSettingAttributes;
    }

    String accountId = inputSettingAttributes.get(0).getAccountId();
    List<SettingAttribute> filteredSettingAttributes = Lists.newArrayList();

    RestrictionsAndAppEnvMap restrictionsAndAppEnvMap =
        usageRestrictionsService.getRestrictionsAndAppEnvMapFromCache(accountId, Action.READ);
    Map<String, Set<String>> appEnvMapFromUserPermissions = restrictionsAndAppEnvMap.getAppEnvMap();
    UsageRestrictions restrictionsFromUserPermissions = restrictionsAndAppEnvMap.getUsageRestrictions();

    boolean isAccountAdmin = usageRestrictionsService.isAccountAdmin(accountId);

    Set<String> appsByAccountId = appService.getAppIdsAsSetByAccountId(accountId);
    Map<String, List<Base>> appIdEnvMap = envService.getAppIdEnvMap(appsByAccountId);

    inputSettingAttributes.forEach(settingAttribute -> {
      UsageRestrictions usageRestrictionsFromEntity = settingAttribute.getUsageRestrictions();

      if (usageRestrictionsService.hasAccess(accountId, isAccountAdmin, appIdFromRequest, envIdFromRequest,
              usageRestrictionsFromEntity, restrictionsFromUserPermissions, appEnvMapFromUserPermissions,
              appIdEnvMap)) {
        // HAR-7726: Mask the encrypted field values when listing all settings.
        SettingValue settingValue = settingAttribute.getValue();
        if (settingValue instanceof EncryptableSetting) {
          secretManager.maskEncryptedFields((EncryptableSetting) settingValue);
        }
        filteredSettingAttributes.add(settingAttribute);
      }
    });

    return filteredSettingAttributes;
  }

  @Override
  @ValidationGroups(Create.class)
  public SettingAttribute save(SettingAttribute settingAttribute) {
    return save(settingAttribute, true);
  }

  @Override
  @ValidationGroups(Create.class)
  public SettingAttribute forceSave(SettingAttribute settingAttribute) {
    usageRestrictionsService.validateUsageRestrictionsOnEntitySave(
        settingAttribute.getAccountId(), settingAttribute.getUsageRestrictions());

    if (settingAttribute.getValue() != null) {
      if (settingAttribute.getValue() instanceof EncryptableSetting) {
        ((EncryptableSetting) settingAttribute.getValue()).setAccountId(settingAttribute.getAccountId());
      }
    }

    return duplicateCheck(()
                              -> wingsPersistence.saveAndGet(SettingAttribute.class, settingAttribute),
        "name", settingAttribute.getName());
  }

  @Override
  public ValidationResult validateConnectivity(SettingAttribute settingAttribute) {
    try {
      SettingAttribute existingSetting = get(settingAttribute.getAppId(), settingAttribute.getUuid());
      if (existingSetting != null) {
        resetUnchangedEncryptedFields(existingSetting, settingAttribute);
      }

      return settingValidationService.validateConnectivity(settingAttribute);
    } catch (Exception ex) {
      return new ValidationResult(false, ExceptionUtils.getMessage(ex));
    }
  }

  private ValidationResult validateInternal(final SettingAttribute settingAttribute) {
    try {
      return new ValidationResult(settingValidationService.validate(settingAttribute), "");
    } catch (Exception ex) {
      return new ValidationResult(false, ExceptionUtils.getMessage(ex));
    }
  }

  @Override
  public ValidationResult validate(final SettingAttribute settingAttribute) {
    return validateInternal(settingAttribute);
  }

  @Override
  public ValidationResult validate(final String varId) {
    final SettingAttribute settingAttribute = get(varId);
    if (settingAttribute != null) {
      return validateInternal(settingAttribute);
    } else {
      return new ValidationResult(false, format("Setting Attribute with id: %s does not exist.", varId));
    }
  }

  @Override
  public Map<String, String> listAccountDefaults(String accountId) {
    return listAccountOrAppDefaults(accountId, GLOBAL_APP_ID);
  }

  @Override
  public Map<String, String> listAppDefaults(String accountId, String appId) {
    return listAccountOrAppDefaults(accountId, appId);
  }

  private Map<String, String> listAccountOrAppDefaults(String accountId, String appId) {
    List<SettingAttribute> settingAttributes = wingsPersistence.createQuery(SettingAttribute.class)
                                                   .filter(ACCOUNT_ID, accountId)
                                                   .filter(APP_ID_KEY, appId)
                                                   .filter(VALUE_TYPE_KEY, SettingVariableTypes.STRING.name())
                                                   .asList();

    return settingAttributes.stream().collect(Collectors.toMap(SettingAttribute::getName,
        settingAttribute
        -> Optional.ofNullable(((StringValue) settingAttribute.getValue()).getValue()).orElse(""),
        (a, b) -> b));
  }

  @Override
  @ValidationGroups(Create.class)
  public SettingAttribute save(SettingAttribute settingAttribute, boolean pushToGit) {
    settingValidationService.validate(settingAttribute);
    // e.g. User is saving GitConnector and setWebhookToken is needed.
    // This fields is populated by us and not by user
    autoGenerateFieldsIfRequired(settingAttribute);
    SettingAttribute newSettingAttribute = forceSave(settingAttribute);

    if (shouldBeSynced(newSettingAttribute, pushToGit)) {
      yamlPushService.pushYamlChangeSet(settingAttribute.getAccountId(), null, newSettingAttribute, Type.CREATE,
          settingAttribute.isSyncFromGit(), false);
    }

    return newSettingAttribute;
  }

  private void autoGenerateFieldsIfRequired(SettingAttribute settingAttribute) {
    if (settingAttribute.getValue() instanceof GitConfig) {
      GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

      if (gitConfig.isGenerateWebhookUrl() && isEmpty(gitConfig.getWebhookToken())) {
        gitConfig.setWebhookToken(CryptoUtil.secureRandAlphaNumString(40));
      }
    }
  }

  private boolean shouldBeSynced(SettingAttribute settingAttribute, boolean pushToGit) {
    String type = settingAttribute.getValue().getType();

    boolean skip = SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES.name().equals(type)
        || SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES.name().equals(type)
        || SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES.name().equals(type);

    return pushToGit && !skip;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#get(java.lang.String, java.lang.String)
   */

  @Override
  public SettingAttribute get(String appId, String varId) {
    return get(appId, GLOBAL_ENV_ID, varId);
  }

  @Override
  public SettingAttribute get(String appId, String envId, String varId) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .filter("appId", appId)
        .filter("envId", envId)
        .filter(ID_KEY, varId)
        .get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#get(java.lang.String)
   */

  @Override
  public SettingAttribute get(String varId) {
    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, varId);
    setInternal(settingAttribute);
    return settingAttribute;
  }

  private void setInternal(SettingAttribute settingAttribute) {
    if (settingAttribute != null && settingAttribute.getValue() instanceof GitConfig) {
      GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
      gitConfigHelperService.setSshKeySettingAttributeIfNeeded(gitConfig);
    }
  }

  @Override
  public SettingAttribute getSettingAttributeByName(String accountId, String settingAttributeName) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .filter("name", settingAttributeName)
        .filter("accountId", accountId)
        .get();
  }

  private void resetUnchangedEncryptedFields(
      SettingAttribute existingSettingAttribute, SettingAttribute newSettingAttribute) {
    if (EncryptableSetting.class.isInstance(existingSettingAttribute.getValue())) {
      EncryptableSetting object = (EncryptableSetting) existingSettingAttribute.getValue();
      object.setDecrypted(false);

      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails(object, newSettingAttribute.getAppId(), null);
      managerDecryptionService.decrypt(object, encryptionDetails);

      secretManager.resetUnchangedEncryptedFields((EncryptableSetting) existingSettingAttribute.getValue(),
          (EncryptableSetting) newSettingAttribute.getValue());
    }
  }

  @Override
  public SettingAttribute update(SettingAttribute settingAttribute, boolean pushToGit) {
    SettingAttribute existingSetting = get(settingAttribute.getAppId(), settingAttribute.getUuid());

    notNullCheck("Setting Attribute was deleted", existingSetting, USER);
    notNullCheck("SettingValue not associated", settingAttribute.getValue(), USER);
    equalCheck(existingSetting.getValue().getType(), settingAttribute.getValue().getType());

    usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(settingAttribute.getAccountId(),
        existingSetting.getUsageRestrictions(), settingAttribute.getUsageRestrictions());

    settingAttribute.setAccountId(existingSetting.getAccountId());
    settingAttribute.setAppId(existingSetting.getAppId());
    // e.g. User is saving GitConnector and setWebhookToken is needed.
    // This fields is populated by us and not by user
    autoGenerateFieldsIfRequired(settingAttribute);

    resetUnchangedEncryptedFields(existingSetting, settingAttribute);
    settingValidationService.validate(settingAttribute);

    SettingAttribute savedSettingAttributes = get(settingAttribute.getUuid());

    ImmutableMap.Builder<String, Object> fields =
        ImmutableMap.<String, Object>builder().put("name", settingAttribute.getName());

    // To revisit
    if (settingAttribute.getUsageRestrictions() != null) {
      fields.put("usageRestrictions", settingAttribute.getUsageRestrictions());
    }

    if (settingAttribute.getValue() != null) {
      if (settingAttribute.getValue() instanceof EncryptableSetting) {
        ((EncryptableSetting) settingAttribute.getValue()).setAccountId(settingAttribute.getAccountId());
      }
      fields.put("value", settingAttribute.getValue());
    }
    wingsPersistence.updateFields(SettingAttribute.class, settingAttribute.getUuid(), fields.build());

    SettingAttribute updatedSettingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttribute.getUuid());

    // Need to mask the privatey key field value before the value is returned.
    // This will avoid confusing the user that the key field is empty when it's not.
    SettingValue updatedSettingValue = updatedSettingAttribute.getValue();
    if (updatedSettingValue instanceof HostConnectionAttributes) {
      HostConnectionAttributes hostConnectionAttributes = (HostConnectionAttributes) updatedSettingValue;
      if (!hostConnectionAttributes.isKeyless()) {
        hostConnectionAttributes.setKey(ENCRYPTED_FIELD_MASK.toCharArray());
      }
    }

    if (shouldBeSynced(updatedSettingAttribute, pushToGit)) {
      boolean isRename = !savedSettingAttributes.getName().equals(updatedSettingAttribute.getName());
      yamlPushService.pushYamlChangeSet(settingAttribute.getAccountId(), savedSettingAttributes,
          updatedSettingAttribute, Type.UPDATE, settingAttribute.isSyncFromGit(), isRename);
    }
    cacheHelper.getNewRelicApplicationCache().remove(updatedSettingAttribute.getUuid());

    return updatedSettingAttribute;
  }

  @Override
  public void updateUsageRestrictionsInternal(String uuid, UsageRestrictions usageRestrictions) {
    ImmutableMap.Builder<String, Object> fields =
        ImmutableMap.<String, Object>builder().put("usageRestrictions", usageRestrictions);
    wingsPersistence.updateFields(SettingAttribute.class, uuid, fields.build());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#update(software.wings.beans.SettingAttribute)
   */

  @Override
  public SettingAttribute update(SettingAttribute settingAttribute) {
    return update(settingAttribute, true);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#delete(java.lang.String, java.lang.String)
   */
  @Override
  public void delete(String appId, String varId) {
    this.delete(appId, varId, true, false);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#delete(java.lang.String, java.lang.String)
   */
  @Override
  public void delete(String appId, String varId, boolean pushToGit, boolean syncFromGit) {
    SettingAttribute settingAttribute = get(varId);
    notNullCheck("Setting Value", settingAttribute, USER);
    String accountId = settingAttribute.getAccountId();
    if (!usageRestrictionsService.userHasPermissionsToChangeEntity(
            accountId, settingAttribute.getUsageRestrictions())) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }

    ensureSettingAttributeSafeToDelete(settingAttribute);

    boolean deleted = wingsPersistence.delete(settingAttribute);
    if (deleted && shouldBeSynced(settingAttribute, pushToGit)) {
      yamlPushService.pushYamlChangeSet(accountId, settingAttribute, null, Type.DELETE, syncFromGit, false);
      cacheHelper.getNewRelicApplicationCache().remove(settingAttribute.getUuid());
    }
  }

  @Override
  public void deleteByYamlGit(String appId, String settingAttributeId, boolean syncFromGit) {
    delete(appId, settingAttributeId, true, syncFromGit);
  }

  private void ensureSettingAttributeSafeToDelete(SettingAttribute settingAttribute) {
    if (settingAttribute.getCategory().equals(SettingCategory.CLOUD_PROVIDER)) {
      ensureCloudProviderSafeToDelete(settingAttribute);
    } else if (settingAttribute.getCategory().equals(SettingCategory.CONNECTOR)) {
      ensureConnectorSafeToDelete(settingAttribute);
    } else if (settingAttribute.getCategory().equals(SettingCategory.SETTING)) {
      ensureSettingSafeToDelete(settingAttribute);
    }
  }

  private void ensureSettingSafeToDelete(SettingAttribute settingAttribute) {
    // TODO:: workflow scan for finding out usage in Steps/expression ???
  }

  private void ensureConnectorSafeToDelete(SettingAttribute connectorSetting) {
    if (SettingVariableTypes.ELB.name().equals(connectorSetting.getValue().getType())) {
      List<InfrastructureMapping> infrastructureMappings =
          infrastructureMappingService
              .list(aPageRequest()
                        .addFilter("loadBalancerId", EQ, connectorSetting.getUuid())
                        .withLimit(PageRequest.UNLIMITED)
                        .build(),
                  excludeValidate)
              .getResponse();

      List<String> infraMappingNames =
          infrastructureMappings.stream().map(InfrastructureMapping::getName).collect(toList());
      if (!infraMappingNames.isEmpty()) {
        throw new InvalidRequestException(format("Connector [%s] is referenced by %d Service %s [%s].",
            connectorSetting.getName(), infraMappingNames.size(), plural("Infrastructure", infraMappingNames.size()),
            Joiner.on(", ").join(infraMappingNames)));
      }
    } else {
      List<ArtifactStream> artifactStreams =
          artifactStreamService
              .list(aPageRequest()
                        .addFilter(ArtifactStream.APP_ID_KEY, EQ, connectorSetting.getAppId())
                        .addFilter("settingId", EQ, connectorSetting.getUuid())
                        .build())
              .getResponse();
      if (!artifactStreams.isEmpty()) {
        List<String> artifactStreamNames = artifactStreams.stream()
                                               .map(ArtifactStream::getSourceName)
                                               .filter(java.util.Objects::nonNull)
                                               .collect(toList());
        throw new InvalidRequestException(
            format("Connector [%s] is referenced by %d Artifact %s [%s].", connectorSetting.getName(),
                artifactStreamNames.size(), plural("Source", artifactStreamNames.size()),
                Joiner.on(", ").join(artifactStreamNames)),
            USER);
      }

      List<Rejection> rejections = manipulationSubject.fireApproveFromAll(
          SettingsServiceManipulationObxxxxxxxx:settingsServiceDeleting, connectorSetting);
      if (isNotEmpty(rejections)) {
        throw new InvalidRequestException(
            format("[%s]", Joiner.on("\n").join(rejections.stream().map(Rejection::message).collect(toList()))), USER);
      }
    }

    // TODO:: workflow scan for finding out usage in Steps ???
  }

  private void ensureCloudProviderSafeToDelete(SettingAttribute cloudProviderSetting) {
    List<InfrastructureMapping> infrastructureMappings = infrastructureMappingService.listByComputeProviderId(
        cloudProviderSetting.getAccountId(), cloudProviderSetting.getUuid());

    if (!infrastructureMappings.isEmpty()) {
      List<String> infraMappingNames =
          infrastructureMappings.stream().map(InfrastructureMapping::getName).collect(toList());
      throw new InvalidRequestException(
          format("Cloud provider [%s] is referenced by %d Service %s [%s].", cloudProviderSetting.getName(),
              infraMappingNames.size(), plural("Infrastructure", infraMappingNames.size()),
              Joiner.on(", ").join(infraMappingNames)),
          USER);
    }

    List<ArtifactStream> artifactStreams = artifactStreamService.listBySettingId(cloudProviderSetting.getUuid());
    if (!artifactStreams.isEmpty()) {
      List<String> artifactStreamNames = artifactStreams.stream().map(ArtifactStream::getName).collect(toList());
      throw new InvalidRequestException(
          format("Cloud provider [%s] is referenced by %d Artifact %s [%s].", cloudProviderSetting.getName(),
              artifactStreamNames.size(), plural("Source", artifactStreamNames.size()),
              Joiner.on(", ").join(artifactStreamNames)),
          USER);
    }

    // TODO:: workflow scan for finding out usage in Steps ???
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#getByName(java.lang.String, java.lang.String)
   */
  @Override
  public SettingAttribute getByName(String accountId, String appId, String attributeName) {
    return getByName(accountId, appId, GLOBAL_ENV_ID, attributeName);
  }

  @Override
  public SettingAttribute getByName(String accountId, String appId, String envId, String attributeName) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .filter("accountId", accountId)
        .field("appId")
        .in(asList(appId, GLOBAL_APP_ID))
        .field("envId")
        .in(asList(envId, GLOBAL_ENV_ID))
        .filter("name", attributeName)
        .get();
  }

  @Override
  public SettingAttribute fetchSettingAttributeByName(
      String accountId, String attributeName, SettingVariableTypes settingVariableTypes) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .filter(SettingAttribute.ACCOUNT_ID_KEY, accountId)
        .filter(SettingAttribute.APP_ID_KEY, GLOBAL_APP_ID)
        .filter(ENV_ID_KEY, GLOBAL_ENV_ID)
        .filter(SettingAttribute.NAME_KEY, attributeName)
        .filter(VALUE_TYPE_KEY, settingVariableTypes.name())
        .get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#createDefaultApplicationSettings(java.lang.String)
   */
  @Override
  public void createDefaultApplicationSettings(String appId, String accountId, boolean syncFromGit) {
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName(WINDOWS_RUNTIME_PATH)
                              .withValue(aStringValue().withValue(DEFAULT_WINDOWS_RUNTIME_PATH).build())
                              .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName(RUNTIME_PATH)
                              .withValue(aStringValue().withValue(DEFAULT_RUNTIME_PATH).build())
                              .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName(STAGING_PATH)
                              .withValue(aStringValue().withValue(DEFAULT_STAGING_PATH).build())
                              .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                              .build());

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withAppId(appId)
                                            .withAccountId(accountId)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withName(BACKUP_PATH)
                                            .withValue(aStringValue().withValue(DEFAULT_BACKUP_PATH).build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    wingsPersistence.save(settingAttribute);

    // We only need to queue one of them since it will fetch all the setting attributes and pushes them
    yamlPushService.pushYamlChangeSet(
        settingAttribute.getAccountId(), null, settingAttribute, Type.CREATE, syncFromGit, false);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#getSettingAttributesByType(java.lang.String,
   * software.wings.settings.SettingValue.SettingVariableTypes)
   */
  @Override
  public List<SettingAttribute> getSettingAttributesByType(String appId, String type) {
    return getSettingAttributesByType(appId, GLOBAL_ENV_ID, type);
  }

  @Override
  public List<SettingAttribute> getFilteredSettingAttributesByType(
      String appId, String type, String currentAppId, String currentEnvId) {
    return getFilteredSettingAttributesByType(appId, GLOBAL_ENV_ID, type, currentAppId, currentEnvId);
  }

  @Override
  public List<SettingAttribute> getSettingAttributesByType(String appId, String envId, String type) {
    PageRequest<SettingAttribute> pageRequest;
    if (appId == null || appId.equals(GLOBAL_APP_ID)) {
      pageRequest = aPageRequest()
                        .addFilter("appId", EQ, GLOBAL_APP_ID)
                        .addFilter("envId", EQ, GLOBAL_ENV_ID)
                        .addFilter("value.type", EQ, type)
                        .build();
    } else {
      Application application = appService.get(appId);
      pageRequest = aPageRequest()
                        .addFilter("accountId", EQ, application.getAccountId())
                        .addFilter("envId", EQ, GLOBAL_ENV_ID)
                        .addFilter("value.type", EQ, type)
                        .build();
    }

    return wingsPersistence.query(SettingAttribute.class, pageRequest).getResponse();
  }

  @Override
  public List<SettingAttribute> getFilteredSettingAttributesByType(
      String appId, String envId, String type, String currentAppId, String currentEnvId) {
    List<SettingAttribute> settingAttributeList = getSettingAttributesByType(appId, envId, type);
    return getFilteredSettingAttributes(settingAttributeList, currentAppId, currentEnvId);
  }

  @Override
  public List<SettingAttribute> getSettingAttributesByType(String accountId, String appId, String envId, String type) {
    List<SettingAttribute> settingAttributes = new ArrayList<>();

    try (HIterator<SettingAttribute> iterator = new HIterator(wingsPersistence.createQuery(SettingAttribute.class)
                                                                  .filter(ACCOUNT_ID, accountId)
                                                                  .filter(APP_ID_KEY, appId)
                                                                  .filter(ENV_ID_KEY, envId)
                                                                  .filter(VALUE_TYPE_KEY, type)
                                                                  .order(NAME_KEY)
                                                                  .fetch())) {
      while (iterator.hasNext()) {
        settingAttributes.add(iterator.next());
      }
    }

    return settingAttributes;
  }

  @Override
  public List<SettingAttribute> getGlobalSettingAttributesByType(String accountId, String type) {
    PageRequest<SettingAttribute> pageRequest =
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter("value.type", EQ, type).build();
    return wingsPersistence.query(SettingAttribute.class, pageRequest).getResponse();
  }

  @Override
  public List<SettingAttribute> getFilteredGlobalSettingAttributesByType(
      String accountId, String type, String currentAppId, String currentEnvId) {
    List<SettingAttribute> settingAttributeList = getGlobalSettingAttributesByType(accountId, type);
    return getFilteredSettingAttributes(settingAttributeList, currentAppId, currentEnvId);
  }

  @Override
  public SettingValue getSettingValueById(String accountId, String id) {
    SettingAttribute settingAttribute = wingsPersistence.createQuery(SettingAttribute.class)
                                            .filter(SettingAttribute.ACCOUNT_ID_KEY, accountId)
                                            .filter(SettingAttribute.ID_KEY, id)
                                            .get();
    if (settingAttribute != null) {
      return settingAttribute.getValue();
    }
    return null;
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class).filter("accountId", accountId));
  }

  @Override
  public void deleteSettingAttributesByType(String accountId, String appId, String envId, String type) {
    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class)
                                .filter("accountId", accountId)
                                .filter("appId", appId)
                                .filter("envId", envId)
                                .filter("value.type", type));
  }

  @Override
  public GitConfig fetchGitConfigFromConnectorId(String gitConnectorId) {
    if (isBlank(gitConnectorId)) {
      return null;
    }

    SettingAttribute gitSettingAttribute = get(gitConnectorId);

    if (gitSettingAttribute == null || !(gitSettingAttribute.getValue() instanceof GitConfig)) {
      throw new InvalidRequestException("Git connector not found", USER);
    }

    GitConfig gitConfig = (GitConfig) gitSettingAttribute.getValue();
    gitConfigHelperService.setSshKeySettingAttributeIfNeeded(gitConfig);
    return gitConfig;
  }
}
