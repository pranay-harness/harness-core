package software.wings.graphql.datafetcher.application;

import software.wings.beans.Application;
import software.wings.graphql.schema.type.QLApplication;

public class ApplicationController {
  public static QLApplication getApplicationInfo(Application application) {
    return QLApplication.builder()
        .id(application.getAppId())
        .name(application.getName())
        .description(application.getDescription())
        .build();
  }
}
