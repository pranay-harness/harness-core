package software.wings.graphql.datafetcher.application;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationInput;
import software.wings.graphql.schema.mutation.application.payload.QLDeleteApplicationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

@Slf4j
public class DeleteApplicationDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateApplicationInput, QLDeleteApplicationPayload> {
  private AppService appService;

  @Inject
  public DeleteApplicationDataFetcher(AppService appService) {
    super(QLUpdateApplicationInput.class, QLDeleteApplicationPayload.class);
    this.appService = appService;
  }

  @Override
  @AuthRule(permissionType = MANAGE_APPLICATIONS, action = PermissionAttribute.Action.DELETE)
  protected QLDeleteApplicationPayload mutateAndFetch(
      QLUpdateApplicationInput parameter, MutationContext mutationContext) {
    appService.delete(parameter.getApplicationId());
    return QLDeleteApplicationPayload.builder().clientMutationId(parameter.getClientMutationId()).build();
  }
}
