package software.wings.graphql.datafetcher.environment;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.govern.Switch.unhandled;

import io.harness.annotations.dev.OwnedBy;
import lombok.experimental.UtilityClass;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;
import software.wings.graphql.schema.type.QLEnvironmentType;

@OwnedBy(CDC)
@UtilityClass
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
        .createdAt(environment.getCreatedAt())
        .createdBy(UserController.populateUser(environment.getCreatedBy()))
        .appId(environment.getAppId());
  }
}
