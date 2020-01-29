package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationGitSyncConfigInput;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationGitSyncConfigInput.QLUpdateApplicationGitSyncConfigInputKeys;
import software.wings.graphql.schema.mutation.application.payload.QLRemoveApplicationGitSyncConfigPayload;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.YamlGitService;

@Slf4j
public class RemoveApplicationGitSyncConfigDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateApplicationGitSyncConfigInput, QLRemoveApplicationGitSyncConfigPayload> {
  private final AppService appService;
  private final YamlGitService yamlGitService;

  @Inject
  public RemoveApplicationGitSyncConfigDataFetcher(AppService appService, YamlGitService yamlGitService) {
    super(QLUpdateApplicationGitSyncConfigInput.class, QLRemoveApplicationGitSyncConfigPayload.class);
    this.appService = appService;
    this.yamlGitService = yamlGitService;
  }

  @Override
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE, action = PermissionAttribute.Action.CREATE)
  protected QLRemoveApplicationGitSyncConfigPayload mutateAndFetch(
      QLUpdateApplicationGitSyncConfigInput input, MutationContext mutationContext) {
    validate(input);
    final Application application = getApplication(input.getApplicationId());
    yamlGitService.delete(mutationContext.getAccountId(), application.getUuid(), EntityType.APPLICATION);

    return QLRemoveApplicationGitSyncConfigPayload.builder()
        .clientMutationId(input.getClientMutationId())
        .application(ApplicationController.populateQLApplication(application, QLApplication.builder()).build())
        .build();
  }

  private void validate(QLUpdateApplicationGitSyncConfigInput input) {
    utils.ensureNotBlankField(input.getApplicationId(), QLUpdateApplicationGitSyncConfigInputKeys.applicationId);
  }

  private Application getApplication(String applicationId) {
    return appService.get(applicationId);
  }
}
