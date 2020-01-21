package software.wings.graphql.datafetcher.user;

import com.google.inject.Inject;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.type.QLRequestStatus;
import software.wings.graphql.schema.type.user.QLDeleteUserInput;
import software.wings.graphql.schema.type.user.QLDeleteUserPayload;
import software.wings.graphql.schema.type.user.QLDeleteUserPayload.QLDeleteUserPayloadBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserService;

public class DeleteUserDataFetcher extends BaseMutatorDataFetcher<QLDeleteUserInput, QLDeleteUserPayload> {
  @Inject private UserService userService;

  @Inject
  public DeleteUserDataFetcher(UserService userService) {
    super(QLDeleteUserInput.class, QLDeleteUserPayload.class);
    this.userService = userService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLDeleteUserPayload mutateAndFetch(QLDeleteUserInput qlDeleteUserInput, MutationContext mutationContext) {
    QLDeleteUserPayloadBuilder qlDeleteUserPayloadBuilder =
        QLDeleteUserPayload.builder().requestId(qlDeleteUserInput.getRequestId());
    try {
      userService.delete(mutationContext.getAccountId(), qlDeleteUserInput.getId());
      return qlDeleteUserPayloadBuilder.status(QLRequestStatus.SUCCESS).build();
    } catch (Exception ex) {
      return qlDeleteUserPayloadBuilder.status(QLRequestStatus.FAILED).message(ex.getMessage()).build();
    }
  }
}