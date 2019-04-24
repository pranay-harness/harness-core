package software.wings.graphql.datafetcher.environment;

import com.google.inject.Singleton;

import software.wings.beans.Environment;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;

@Singleton
public class EnvironmentController {
  public static void populateEnvironment(Environment environment, QLEnvironmentBuilder builder) {
    builder.id(environment.getUuid())
        .name(environment.getName())
        .description(environment.getDescription())
        .type(environment.getEnvironmentType().name());
  }
}
