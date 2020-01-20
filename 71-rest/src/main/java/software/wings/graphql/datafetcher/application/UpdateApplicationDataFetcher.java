package software.wings.graphql.datafetcher.application;

import static java.lang.String.format;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.utils.RequestField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationInput;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationInput.QLUpdateApplicationInputKeys;
import software.wings.graphql.schema.mutation.application.payload.QLUpdateApplicationPayload;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

@Slf4j
public class UpdateApplicationDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateApplicationInput, QLUpdateApplicationPayload> {
  private AppService appService;

  @Inject
  public UpdateApplicationDataFetcher(AppService appService) {
    super(QLUpdateApplicationInput.class, QLUpdateApplicationPayload.class);
    this.appService = appService;
  }

  private Application prepareApplication(
      QLUpdateApplicationInput qlUpdateApplicationInput, Application existingApplication) {
    final Application.Builder applicationBuilder =
        anApplication()
            .uuid(existingApplication.getUuid())
            .appId(existingApplication.getAppId())
            .accountId(existingApplication.getAccountId())
            .name(existingApplication.getName())
            .description(existingApplication.getDescription())
            .yamlGitConfig(existingApplication.getYamlGitConfig()); // yaml config because the way update is written, it
                                                                    // assumes this would be coming

    if (qlUpdateApplicationInput.getName().hasBeenSet()) {
      applicationBuilder.name(qlUpdateApplicationInput.getName().getValue().map(StringUtils::strip).orElse(null));
    }
    if (qlUpdateApplicationInput.getDescription().hasBeenSet()) {
      applicationBuilder.description(qlUpdateApplicationInput.getDescription().getValue().orElse(null));
    }

    return applicationBuilder.build();
  }

  private QLApplication prepareQLApplication(Application savedApplication) {
    return ApplicationController.populateQLApplication(savedApplication, QLApplication.builder()).build();
  }

  @Override
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE, action = PermissionAttribute.Action.UPDATE)
  protected QLUpdateApplicationPayload mutateAndFetch(
      QLUpdateApplicationInput parameter, MutationContext mutationContext) {
    validate(parameter);
    final Application existingApplication = appService.get(parameter.getApplicationId());
    final Application updatedApp = appService.update(prepareApplication(parameter, existingApplication));
    return QLUpdateApplicationPayload.builder()
        .requestId(parameter.getRequestId())
        .application(prepareQLApplication(updatedApp))
        .build();
  }

  private void validate(QLUpdateApplicationInput parameter) {
    final RequestField<String> nameRF = parameter.getName();
    if (nameRF.hasBeenSet()) {
      ensureNonEmptyStringField(nameRF.getValue().orElse(null), QLUpdateApplicationInputKeys.name);
    }
  }

  private void ensureNonEmptyStringField(String field, String fieldName) {
    if (StringUtils.isBlank(field)) {
      throw new InvalidRequestException(format("Field: [%s] is required", fieldName));
    }
  }
}
