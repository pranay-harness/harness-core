package software.wings.graphql.datafetcher.application;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trim;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.QLUpdateApplicationParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplicationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

@Slf4j
public class UpdateApplicationDataFetcher extends BaseMutatorDataFetcher<QLUpdateApplicationParameters, QLApplication> {
  @Inject private AppService appService;

  @Inject
  public UpdateApplicationDataFetcher(AppService appService) {
    super(QLUpdateApplicationParameters.class, QLApplication.class);
    this.appService = appService;
  }

  private Application createApplicationToUpdate(
      QLUpdateApplicationParameters qlUpdateApplicationParameters, String accountId, Application existingApplication) {
    final QLApplicationInput applicationInput = qlUpdateApplicationParameters.getApplication();
    return Application.Builder.anApplication()
        .appId(qlUpdateApplicationParameters.getApplicationId())
        .name(trim(applicationInput.getName()))
        .description(trim(applicationInput.getDescription()))
        .accountId(accountId)
        .appId(existingApplication.getAppId())
        .uuid(existingApplication.getUuid())
        .build();
  }

  private QLApplication prepareQLApplication(Application savedApplication) {
    return ApplicationController.populateQLApplication(savedApplication, QLApplication.builder()).build();
  }

  @Override
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE, action = PermissionAttribute.Action.UPDATE)
  protected QLApplication mutateAndFetch(QLUpdateApplicationParameters parameter, MutationContext mutationContext) {
    validate(parameter);
    final Application existingApplication = appService.get(parameter.getApplicationId());
    final Application updatedApp =
        appService.update(createApplicationToUpdate(parameter, mutationContext.getAccountId(), existingApplication));

    return prepareQLApplication(updatedApp);
  }

  private void validate(QLUpdateApplicationParameters parameter) {
    ensureNonEmptyStringField(parameter.getApplicationId(), "applicationId");
    ensureNotNullField(parameter.getApplication(), "application");
    ensureNonEmptyStringField(parameter.getApplication().getName(), "application.name");
  }

  private void ensureNonEmptyStringField(String field, String fieldName) {
    if (isEmpty(field)) {
      throw new InvalidRequestException(format("Field: [%s] is required", fieldName));
    }
  }

  private void ensureNotNullField(Object field, String fieldName) {
    if (field == null) {
      throw new InvalidRequestException(format("Field: [%s] is required", fieldName));
    }
  }
}
