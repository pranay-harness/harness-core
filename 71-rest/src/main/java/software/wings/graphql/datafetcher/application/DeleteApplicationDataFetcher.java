package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.QLDeleteApplicationResult;
import software.wings.graphql.schema.mutation.application.QLUpdateApplicationParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

@Slf4j
public class DeleteApplicationDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateApplicationParameters, QLDeleteApplicationResult> {
  @Inject private AppService appService;

  @Inject
  public DeleteApplicationDataFetcher(AppService appService) {
    super(QLUpdateApplicationParameters.class, QLDeleteApplicationResult.class);
    this.appService = appService;
  }

  @Override
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE, action = PermissionAttribute.Action.DELETE)
  protected QLDeleteApplicationResult mutateAndFetch(
      QLUpdateApplicationParameters parameter, MutationContext mutationContext) {
    appService.delete(parameter.getApplicationId());
    return QLDeleteApplicationResult.builder().success(Boolean.TRUE).build();
  }
}
