package software.wings.graphql.datafetcher.application;

import lombok.experimental.UtilityClass;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;

@UtilityClass
public class ApplicationController {
  public static void populateApplication(Application application, QLApplicationBuilder builder) {
    builder.id(application.getAppId())
        .name(application.getName())
        .description(application.getDescription())
        .createdAt(application.getCreatedAt())
        .createdBy(UserController.populateUser(application.getCreatedBy()));
  }
}
