package software.wings.graphql.datafetcher.application;

import software.wings.beans.Application;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApplicationController {
  public static QLApplicationBuilder populateQLApplication(Application application, QLApplicationBuilder builder) {
    return builder.id(application.getAppId())
        .name(application.getName())
        .description(application.getDescription())
        .createdAt(application.getCreatedAt())
        .createdBy(UserController.populateUser(application.getCreatedBy()));
  }
}
