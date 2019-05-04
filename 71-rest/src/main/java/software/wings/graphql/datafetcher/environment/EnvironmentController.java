package software.wings.graphql.datafetcher.environment;

import static io.harness.govern.Switch.unhandled;

import com.google.inject.Singleton;

import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;
import software.wings.graphql.schema.type.QLEnvironmentType;

@Singleton
public class EnvironmentController {
  public static QLEnvironmentType convertEnvironmentType(EnvironmentType type) {
    switch (type) {
      case PROD:
        return QLEnvironmentType.PROD;
      case NON_PROD:
        return QLEnvironmentType.NON_PROD;
      default:
        unhandled(type);
    }
    return null;
  }

  public static void populateEnvironment(Environment environment, QLEnvironmentBuilder builder) {
    builder.id(environment.getUuid())
        .name(environment.getName())
        .description(environment.getDescription())
        .type(convertEnvironmentType(environment.getEnvironmentType()))
        .createdAt(GraphQLDateTimeScalar.convert(environment.getCreatedAt()))
        .createdBy(UserController.populateUser(environment.getCreatedBy()));
  }
}
