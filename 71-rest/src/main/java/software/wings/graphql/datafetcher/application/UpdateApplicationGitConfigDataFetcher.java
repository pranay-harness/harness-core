package software.wings.graphql.datafetcher.application;

import static org.apache.commons.lang3.StringUtils.strip;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationGitConfigInput;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationGitConfigInput.QLUpdateApplicationGitConfigInputKeys;
import software.wings.graphql.schema.mutation.application.payload.QLUpdateApplicationGitConfigPayload;
import software.wings.graphql.schema.type.QLGitConfig;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.YamlGitConfigBuilder;

import java.util.Optional;

@Slf4j
public class UpdateApplicationGitConfigDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateApplicationGitConfigInput, QLUpdateApplicationGitConfigPayload> {
  private final AppService appService;
  private final YamlGitService yamlGitService;
  private HPersistence persistence;
  @Inject
  public UpdateApplicationGitConfigDataFetcher(
      AppService appService, YamlGitService yamlGitService, HPersistence persistence) {
    super(QLUpdateApplicationGitConfigInput.class, QLUpdateApplicationGitConfigPayload.class);
    this.appService = appService;
    this.yamlGitService = yamlGitService;
    this.persistence = persistence;
  }

  @Override
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE, action = PermissionAttribute.Action.CREATE)
  protected QLUpdateApplicationGitConfigPayload mutateAndFetch(
      QLUpdateApplicationGitConfigInput input, MutationContext mutationContext) {
    validate(input);
    final Application application = getApplication(input.getApplicationId());
    validateGitConnector(strip(input.getGitConnectorId()), mutationContext.getAccountId());
    final YamlGitConfig savedYamlGitConfig =
        getSavedYamlGitConfig(input.getApplicationId(), mutationContext.getAccountId());

    YamlGitConfig updatedYamlGitConfig;
    if (savedYamlGitConfig == null) {
      // create new
      updatedYamlGitConfig = createAndSaveYamlGitConfig(input, application);
    } else {
      // update existing
      updatedYamlGitConfig = updateYamlGitConfig(input, savedYamlGitConfig);
    }

    return QLUpdateApplicationGitConfigPayload.builder()
        .requestId(input.getRequestId())
        .gitConfig(getGitConfigFrom(updatedYamlGitConfig))
        .build();
  }
  private QLGitConfig getGitConfigFrom(YamlGitConfig yamlGitConfig) {
    return YamlGitConfigController.populateQLGitConfig(yamlGitConfig, QLGitConfig.builder()).build();
  }

  private void validateGitConnector(String connectorId, String accountId) {
    SettingAttribute settingAttribute = persistence.get(SettingAttribute.class, connectorId);
    if (settingAttribute == null) {
      throw new InvalidRequestException("Git Connector does not exist", WingsException.USER);
    }

    if (!settingAttribute.getAccountId().equals(accountId)) {
      throw new InvalidRequestException("Git Connector does not exist", WingsException.USER);
    }

    if (!(SettingCategory.CONNECTOR == settingAttribute.getCategory()
            && SettingVariableTypes.GIT
                == Optional.of(settingAttribute)
                       .map(SettingAttribute::getValue)
                       .map(SettingValue::getSettingType)
                       .orElse(null))) {
      throw new InvalidRequestException("Connector should be a Git Connector", WingsException.USER);
    }
  }

  private YamlGitConfig updateYamlGitConfig(QLUpdateApplicationGitConfigInput input, YamlGitConfig savedYamlGitConfig) {
    savedYamlGitConfig.setGitConnectorId(input.getGitConnectorId());
    savedYamlGitConfig.setBranchName(input.getBranch());
    savedYamlGitConfig.setEnabled(input.getSyncEnabled());
    return yamlGitService.update(savedYamlGitConfig);
  }

  private void validate(QLUpdateApplicationGitConfigInput input) {
    utils.ensureNotBlankField(input.getApplicationId(), QLUpdateApplicationGitConfigInputKeys.applicationId);
    utils.ensureNotBlankField(input.getGitConnectorId(), QLUpdateApplicationGitConfigInputKeys.gitConnectorId);
    utils.ensureNotBlankField(input.getBranch(), QLUpdateApplicationGitConfigInputKeys.branch);
    utils.ensureNotBlankField(input.getRequestId(), QLUpdateApplicationGitConfigInputKeys.requestId);
    utils.ensureNotNullField(input.getSyncEnabled(), QLUpdateApplicationGitConfigInputKeys.syncEnabled);
  }

  private YamlGitConfig getSavedYamlGitConfig(String applicationId, String accountId) {
    return yamlGitService.get(accountId, applicationId, EntityType.APPLICATION);
  }

  private Application getApplication(String applicationId) {
    return appService.get(applicationId);
  }

  private YamlGitConfig createAndSaveYamlGitConfig(QLUpdateApplicationGitConfigInput input, Application application) {
    final YamlGitConfigBuilder configBuilder = YamlGitConfig.builder();
    final YamlGitConfig yamlGitConfig = configBuilder.accountId(application.getAccountId())
                                            .gitConnectorId(strip(input.getGitConnectorId()))
                                            .branchName(strip(input.getBranch()))
                                            .enabled(input.getSyncEnabled())
                                            .syncMode(YamlGitConfig.SyncMode.BOTH)
                                            .entityType(EntityType.APPLICATION)
                                            .entityId(application.getUuid())
                                            .build();
    yamlGitConfig.setAppId(application.getAppId());
    return yamlGitService.save(yamlGitConfig);
  }
}
