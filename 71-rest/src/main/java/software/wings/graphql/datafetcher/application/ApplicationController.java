package software.wings.graphql.datafetcher.application;

import software.wings.beans.Application;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;

public class ApplicationController {
  public static void populateApplication(Application application, QLApplicationBuilder builder) {
    builder.id(application.getAppId()).name(application.getName()).description(application.getDescription());
  }
}
