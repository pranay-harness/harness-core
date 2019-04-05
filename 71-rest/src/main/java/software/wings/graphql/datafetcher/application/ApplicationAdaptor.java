package software.wings.graphql.datafetcher.application;

import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.graphql.schema.type.ApplicationInfo;

@Singleton
public class ApplicationAdaptor {
  public ApplicationInfo getApplicationInfo(Application application) {
    return ApplicationInfo.builder()
        .id(application.getAppId())
        .name(application.getName())
        .description(application.getDescription())
        .build();
  }
}
