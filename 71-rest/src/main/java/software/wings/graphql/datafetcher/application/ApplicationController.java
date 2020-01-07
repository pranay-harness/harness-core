package software.wings.graphql.datafetcher.application;

import lombok.experimental.UtilityClass;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;
import software.wings.graphql.schema.type.QLApplicationInput;

@UtilityClass
public class ApplicationController {
  public static QLApplicationBuilder populateQLApplication(Application application, QLApplicationBuilder builder) {
    return builder.id(application.getAppId())
        .name(application.getName())
        .description(application.getDescription())
        .createdAt(application.getCreatedAt())
        .createdBy(UserController.populateUser(application.getCreatedBy()));
  }

  public static Application.Builder populateApplication(
      Application.Builder applicationBuilder, QLApplicationInput qlApplicationInput) {
    return applicationBuilder.name(qlApplicationInput.getName()).description(qlApplicationInput.getDescription());
  }
}
