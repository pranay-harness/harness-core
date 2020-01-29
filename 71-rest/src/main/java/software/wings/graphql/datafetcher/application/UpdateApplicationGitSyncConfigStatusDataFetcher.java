package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationGitSyncConfigInput;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationGitSyncConfigInput.QLUpdateApplicationGitSyncConfigInputKeys;
import software.wings.graphql.schema.mutation.application.payload.QLUpdateApplicationGitSyncConfigPayload;
import software.wings.graphql.schema.type.QLGitSyncConfig;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlGitConfig;

@Slf4j
public class UpdateApplicationGitSyncConfigStatusDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateApplicationGitSyncConfigInput, QLUpdateApplicationGitSyncConfigPayload> {
  private final AppService appService;
  private final YamlGitService yamlGitService;

  @Inject
  public UpdateApplicationGitSyncConfigStatusDataFetcher(AppService appService, YamlGitService yamlGitService) {
    super(QLUpdateApplicationGitSyncConfigInput.class, QLUpdateApplicationGitSyncConfigPayload.class);
    this.appService = appService;
    this.yamlGitService = yamlGitService;
  }

  @Override
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE, action = PermissionAttribute.Action.CREATE)
  protected QLUpdateApplicationGitSyncConfigPayload mutateAndFetch(
      QLUpdateApplicationGitSyncConfigInput input, MutationContext mutationContext) {
    validate(input);
    final Application application = getApplication(input.getApplicationId());

    final YamlGitConfig savedYamlGitConfig =
        getSavedYamlGitConfig(input.getApplicationId(), mutationContext.getAccountId());

    if (savedYamlGitConfig == null) {
      throw new InvalidRequestException(
          "Git not configured for application Id " + application.getUuid(), WingsException.USER);
    }

    final YamlGitConfig updatedYamlGitConfig = updateYamlGitConfig(input, savedYamlGitConfig);

    return QLUpdateApplicationGitSyncConfigPayload.builder()
        .clientMutationId(input.getClientMutationId())
        .gitSyncConfig(getGitConfigFrom(updatedYamlGitConfig))

        .build();
  }

  private QLGitSyncConfig getGitConfigFrom(YamlGitConfig yamlGitConfig) {
    return YamlGitConfigController.populateQLGitConfig(yamlGitConfig, QLGitSyncConfig.builder()).build();
  }

  private YamlGitConfig updateYamlGitConfig(
      QLUpdateApplicationGitSyncConfigInput input, YamlGitConfig savedYamlGitConfig) {
    savedYamlGitConfig.setEnabled(input.getSyncEnabled());
    return yamlGitService.update(savedYamlGitConfig);
  }

  private void validate(QLUpdateApplicationGitSyncConfigInput input) {
    utils.ensureNotBlankField(input.getApplicationId(), QLUpdateApplicationGitSyncConfigInputKeys.applicationId);
    utils.ensureNotNullField(input.getSyncEnabled(), QLUpdateApplicationGitSyncConfigInputKeys.syncEnabled);
  }

  private YamlGitConfig getSavedYamlGitConfig(String applicationId, String accountId) {
    return yamlGitService.get(accountId, applicationId, EntityType.APPLICATION);
  }

  private Application getApplication(String applicationId) {
    return appService.get(applicationId);
  }
}
