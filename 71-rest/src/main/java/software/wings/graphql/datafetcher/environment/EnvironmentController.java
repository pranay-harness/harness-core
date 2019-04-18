package software.wings.graphql.datafetcher.environment;

import com.google.inject.Singleton;

import software.wings.beans.Environment;
import software.wings.graphql.schema.type.QLEnvironment;

@Singleton
public class EnvironmentController {
  public static QLEnvironment getEnvironmentInfo(Environment environment) {
    return QLEnvironment.builder()
        .id(environment.getUuid())
        .name(environment.getName())
        .description(environment.getDescription())
        .environmentType(environment.getEnvironmentType().name())
        .build();
  }
}
