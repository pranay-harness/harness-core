package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.atteo.evo.inflector.English.plural;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD_SU_APP_USER;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.StringValue.Builder.aStringValue;
import static software.wings.common.Constants.BACKUP_PATH;
import static software.wings.common.Constants.DEFAULT_BACKUP_PATH;
import static software.wings.common.Constants.DEFAULT_RUNTIME_PATH;
import static software.wings.common.Constants.DEFAULT_STAGING_PATH;
import static software.wings.common.Constants.DEFAULT_WINDOWS_RUNTIME_PATH;
import static software.wings.common.Constants.RUNTIME_PATH;
import static software.wings.common.Constants.STAGING_PATH;
import static software.wings.common.Constants.WINDOWS_RUNTIME_PATH;
import static software.wings.dl.HQuery.excludeValidate;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.observer.Rejection;
import io.harness.observer.Subject;
import io.harness.validation.Create;
import lombok.Getter;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.annotation.Encryptable;
import software.wings.beans.Application;
import software.wings.beans.Event.Type;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.ValidationResult;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
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
import software.wings.utils.Misc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 5/17/16.
 */
@ValidateOnExecution
@Singleton
public class SettingsServiceImpl implements SettingsService {
  private static final Logger logger = LoggerFactory.getLogger(SettingsServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingValidationService settingValidationService;
  @Inject private AppService appService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Transient @Inject private SecretManager secretManager;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private YamlPushService yamlPushService;

  @Getter private Subject<SettingsServiceManipulationObserver> manipulationSubject = new Subject<>();
  @Inject private CacheHelper cacheHelper;

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
      throw new InvalidRequestException(Misc.getMessage(e), e);
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

    inputSettingAttributes.forEach(settingAttribute -> {
      UsageRestrictions usageRestrictionsFromEntity = settingAttribute.getUsageRestrictions();

      if (usageRestrictionsFromEntity == null) {
        filteredSettingAttributes.add(settingAttribute);
        return;
      }

      // Observed some entities having empty usage restrictions. Covering that case.
      // Could have been due to a ui bug at some point.
      if (isEmpty(usageRestrictionsFromEntity.getAppEnvRestrictions())) {
        usageRestrictionsFromEntity.setEditable(true);
        filteredSettingAttributes.add(settingAttribute);
        return;
      }

      Map<String, Set<String>> appEnvMapFromEntityRestrictions =
          usageRestrictionsService.getAppEnvMap(accountId, usageRestrictionsFromEntity.getAppEnvRestrictions());

      if (usageRestrictionsService.hasAccess(accountId, appIdFromRequest, envIdFromRequest, usageRestrictionsFromEntity,
              appEnvMapFromEntityRestrictions, restrictionsFromUserPermissions, appEnvMapFromUserPermissions)) {
        /* usageRestrictionsFromEntity.setEditable(usageRestrictionsService.userHasPermissionsToChangeEntity(
             settingAttribute.getAccountId(), usageRestrictionsFromEntity, appEnvMapFromEntityRestrictions,
             restrictionsFromUserPermissions, appEnvMapFromUserPermissions));*/
        usageRestrictionsFromEntity.setEditable(true);
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
      if (settingAttribute.getValue() instanceof Encryptable) {
        ((Encryptable) settingAttribute.getValue()).setAccountId(settingAttribute.getAccountId());
      }
    }

    return duplicateCheck(()
                              -> wingsPersistence.saveAndGet(SettingAttribute.class, settingAttribute),
        "name", settingAttribute.getName());
  }

  private ValidationResult validateInternal(final SettingAttribute settingAttribute) {
    try {
      return new ValidationResult(settingValidationService.validate(settingAttribute), "");
    } catch (Exception ex) {
      return new ValidationResult(false, Misc.getMessage(ex));
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
  public List<SettingAttribute> listApplicationDefaults(String accountId, String appId) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(APP_ID_KEY, appId)
        .asList();
  }

  @Override
  @ValidationGroups(Create.class)
  public SettingAttribute save(SettingAttribute settingAttribute, boolean pushToGit) {
    settingValidationService.validate(settingAttribute);
    SettingAttribute newSettingAttribute = forceSave(settingAttribute);

    if (shouldBeSynced(newSettingAttribute, pushToGit)) {
      yamlPushService.pushYamlChangeSet(settingAttribute.getAccountId(), null, newSettingAttribute, Type.CREATE,
          settingAttribute.isSyncFromGit(), false);
    }

    return newSettingAttribute;
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
    return wingsPersistence.get(SettingAttribute.class, varId);
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
    if (existingSettingAttribute.getValue() instanceof Encryptable) {
      secretManager.resetUnchangedEncryptedFields(
          (Encryptable) existingSettingAttribute.getValue(), (Encryptable) newSettingAttribute.getValue());
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

    if (Encryptable.class.isInstance(existingSetting.getValue())) {
      Encryptable object = (Encryptable) existingSetting.getValue();
      object.setDecrypted(false);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails(object, settingAttribute.getAppId(), null);
      managerDecryptionService.decrypt(object, encryptionDetails);
    }

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
      if (settingAttribute.getValue() instanceof Encryptable) {
        ((Encryptable) settingAttribute.getValue()).setAccountId(settingAttribute.getAccountId());
      }
      fields.put("value", settingAttribute.getValue());
    }
    wingsPersistence.updateFields(SettingAttribute.class, settingAttribute.getUuid(), fields.build());

    SettingAttribute updatedSettingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttribute.getUuid());

    if (shouldBeSynced(updatedSettingAttribute, pushToGit)) {
      boolean isRename = !savedSettingAttributes.getName().equals(updatedSettingAttribute.getName());
      yamlPushService.pushYamlChangeSet(settingAttribute.getAccountId(), savedSettingAttributes,
          updatedSettingAttribute, Type.UPDATE, settingAttribute.isSyncFromGit(), isRename);
    }
    cacheHelper.getNewRelicApplicationCache().remove(updatedSettingAttribute.getUuid());
    return updatedSettingAttribute;
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
    if (settingAttribute.getCategory().equals(Category.CLOUD_PROVIDER)) {
      ensureCloudProviderSafeToDelete(settingAttribute);
    } else if (settingAttribute.getCategory().equals(Category.CONNECTOR)) {
      ensureConnectorSafeToDelete(settingAttribute);
    } else if (settingAttribute.getCategory().equals(Category.SETTING)) {
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
    List<InfrastructureMapping> infrastructureMappings =
        infrastructureMappingService
            .list(aPageRequest()
                      .addFilter(InfrastructureMapping.APP_ID_KEY, EQ, cloudProviderSetting.getAppId())
                      .addFilter("computeProviderSettingId", EQ, cloudProviderSetting.getUuid())
                      .withLimit(PageRequest.UNLIMITED)
                      .build())
            .getResponse();
    if (!infrastructureMappings.isEmpty()) {
      List<String> infraMappingNames =
          infrastructureMappings.stream().map(InfrastructureMapping::getName).collect(toList());
      throw new InvalidRequestException(
          format("Cloud provider [%s] is referenced by %d Service %s [%s].", cloudProviderSetting.getName(),
              infraMappingNames.size(), plural("Infrastructure", infraMappingNames.size()),
              Joiner.on(", ").join(infraMappingNames)),
          USER);
    }

    List<ArtifactStream> artifactStreams =
        artifactStreamService
            .list(aPageRequest()
                      .addFilter(ArtifactStream.APP_ID_KEY, EQ, cloudProviderSetting.getAppId())
                      .addFilter("settingId", EQ, cloudProviderSetting.getUuid())
                      .withLimit(PageRequest.UNLIMITED)
                      .build())
            .getResponse();
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
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName(RUNTIME_PATH)
                              .withValue(aStringValue().withValue(DEFAULT_RUNTIME_PATH).build())
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName(STAGING_PATH)
                              .withValue(aStringValue().withValue(DEFAULT_STAGING_PATH).build())
                              .build());

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withAppId(appId)
                                            .withAccountId(accountId)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withName(BACKUP_PATH)
                                            .withValue(aStringValue().withValue(DEFAULT_BACKUP_PATH).build())
                                            .build();
    wingsPersistence.save(settingAttribute);

    // We only need to queue one of them since it will fetch all the setting attributes and pushes them
    yamlPushService.pushYamlChangeSet(
        settingAttribute.getAccountId(), null, settingAttribute, Type.CREATE, syncFromGit, false);
  }

  @Override
  public void createDefaultAccountSettings(String accountId) {
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(GLOBAL_APP_ID)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName("User/Password")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withAccessType(USER_PASSWORD)
                                             .withAccountId(accountId)
                                             .build())
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(GLOBAL_APP_ID)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName("User/Password :: su - <app-account>")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withAccessType(USER_PASSWORD_SU_APP_USER)
                                             .withAccountId(accountId)
                                             .build())
                              .build());

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withAppId(GLOBAL_APP_ID)
                                            .withAccountId(accountId)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withName("User/Password :: sudo - <app-account>")
                                            .withValue(aHostConnectionAttributes()
                                                           .withConnectionType(SSH)
                                                           .withAccessType(USER_PASSWORD_SUDO_APP_USER)
                                                           .withAccountId(accountId)
                                                           .build())
                                            .build();
    wingsPersistence.save(settingAttribute);

    // We only need to queue one of them since it will fetch all the setting attributes and pushes them
    yamlPushService.pushYamlChangeSet(
        settingAttribute.getAccountId(), null, settingAttribute, Type.CREATE, false, false);
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
    PageRequest<SettingAttribute> pageRequest = aPageRequest()
                                                    .addFilter("accountId", EQ, accountId)
                                                    .addFilter("appId", EQ, appId)
                                                    .addFilter("envId", EQ, envId)
                                                    .addFilter("value.type", EQ, type)
                                                    .build();
    return wingsPersistence.query(SettingAttribute.class, pageRequest).getResponse();
  }

  @Override
  public List<SettingAttribute> getFilteredSettingAttributesByType(
      String accountId, String appId, String envId, String type, String currentAppId, String currentEnvId) {
    List<SettingAttribute> settingAttributeList = getSettingAttributesByType(accountId, appId, envId, type);
    return getFilteredSettingAttributes(settingAttributeList, currentAppId, currentEnvId);
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
}
