package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLCreateApplicationInput;
import software.wings.graphql.schema.mutation.application.payload.QLCreateApplicationPayload;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

@Slf4j
public class CreateApplicationDataFetcher
    extends BaseMutatorDataFetcher<QLCreateApplicationInput, QLCreateApplicationPayload> {
  private AppService appService;

  @Inject
  public CreateApplicationDataFetcher(AppService appService) {
    super(QLCreateApplicationInput.class, QLCreateApplicationPayload.class);
    this.appService = appService;
  }

  private Application prepareApplication(QLCreateApplicationInput qlApplicationInput, String accountId) {
    return Application.Builder.anApplication()
        .name(qlApplicationInput.getName())
        .description(qlApplicationInput.getDescription())
        .accountId(accountId)
        .build();
  }
  private QLApplication prepareQLApplication(Application savedApplication) {
    return ApplicationController.populateQLApplication(savedApplication, QLApplication.builder()).build();
  }

  @Override
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE, action = PermissionAttribute.Action.CREATE)
  protected QLCreateApplicationPayload mutateAndFetch(
      QLCreateApplicationInput parameter, MutationContext mutationContext) {
    final Application savedApplication = appService.save(prepareApplication(parameter, mutationContext.getAccountId()));
    return QLCreateApplicationPayload.builder()
        .requestId(parameter.getRequestId())
        .application(prepareQLApplication(savedApplication))
        .build();
  }
}
