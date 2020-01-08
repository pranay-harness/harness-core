package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.QLCreateApplicationParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplicationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

@Slf4j
public class CreateApplicationDataFetcher extends BaseMutatorDataFetcher<QLCreateApplicationParameters, QLApplication> {
  @Inject private AppService appService;

  @Inject
  public CreateApplicationDataFetcher(AppService appService) {
    super(QLCreateApplicationParameters.class, QLApplication.class);
    this.appService = appService;
  }

  private Application prepareApplication(QLApplicationInput qlApplicationInput, String accountId) {
    return ApplicationController.populateApplication(Application.Builder.anApplication(), qlApplicationInput)
        .accountId(accountId)
        .build();
  }
  private QLApplication prepareQLApplication(Application savedApplication) {
    return ApplicationController.populateQLApplication(savedApplication, QLApplication.builder()).build();
  }

  @Override
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE, action = PermissionAttribute.Action.CREATE)
  protected QLApplication mutateAndFetch(QLCreateApplicationParameters parameter, MutationContext mutationContext) {
    final Application savedApplication =
        appService.save(prepareApplication(parameter.getApplication(), mutationContext.getAccountId()));

    return prepareQLApplication(savedApplication);
  }
}
