package software.wings.graphql.datafetcher.user;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
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
    final String userId = qlDeleteUserInput.getId();
    try {
      userService.delete(mutationContext.getAccountId(), userId);
      return qlDeleteUserPayloadBuilder.build();
    } catch (Exception ex) {
      throw new InvalidRequestException("User not found");
    }
  }
}