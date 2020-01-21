package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
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
import software.wings.yaml.gitSync.YamlGitConfig;

@Slf4j
public class UpdateApplicationGitConfigStatusDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateApplicationGitConfigInput, QLUpdateApplicationGitConfigPayload> {
  private final AppService appService;
  private final YamlGitService yamlGitService;

  @Inject
  public UpdateApplicationGitConfigStatusDataFetcher(AppService appService, YamlGitService yamlGitService) {
    super(QLUpdateApplicationGitConfigInput.class, QLUpdateApplicationGitConfigPayload.class);
    this.appService = appService;
    this.yamlGitService = yamlGitService;
  }

  @Override
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE, action = PermissionAttribute.Action.CREATE)
  protected QLUpdateApplicationGitConfigPayload mutateAndFetch(
      QLUpdateApplicationGitConfigInput input, MutationContext mutationContext) {
    validate(input);
    final Application application = getApplication(input.getApplicationId());

    final YamlGitConfig savedYamlGitConfig =
        getSavedYamlGitConfig(input.getApplicationId(), mutationContext.getAccountId());

    if (savedYamlGitConfig == null) {
      throw new InvalidRequestException(
          "Git not configured for application Id " + application.getUuid(), WingsException.USER);
    }

    final YamlGitConfig updatedYamlGitConfig = updateYamlGitConfig(input, savedYamlGitConfig);

    return QLUpdateApplicationGitConfigPayload.builder()
        .requestId(input.getRequestId())
        .gitConfig(getGitConfigFrom(updatedYamlGitConfig))

        .build();
  }

  private QLGitConfig getGitConfigFrom(YamlGitConfig yamlGitConfig) {
    return YamlGitConfigController.populateQLGitConfig(yamlGitConfig, QLGitConfig.builder()).build();
  }

  private YamlGitConfig updateYamlGitConfig(QLUpdateApplicationGitConfigInput input, YamlGitConfig savedYamlGitConfig) {
    savedYamlGitConfig.setEnabled(input.getSyncEnabled());
    return yamlGitService.update(savedYamlGitConfig);
  }

  private void validate(QLUpdateApplicationGitConfigInput input) {
    utils.ensureNotBlankField(input.getApplicationId(), QLUpdateApplicationGitConfigInputKeys.applicationId);
    utils.ensureNotBlankField(input.getRequestId(), QLUpdateApplicationGitConfigInputKeys.requestId);
    utils.ensureNotNullField(input.getSyncEnabled(), QLUpdateApplicationGitConfigInputKeys.syncEnabled);
  }

  private YamlGitConfig getSavedYamlGitConfig(String applicationId, String accountId) {
    return yamlGitService.get(accountId, applicationId, EntityType.APPLICATION);
  }

  private Application getApplication(String applicationId) {
    return appService.get(applicationId);
  }
}
