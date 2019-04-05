package software.wings.graphql.datafetcher.environment;

import com.google.inject.Singleton;

import software.wings.beans.Environment;
import software.wings.graphql.schema.type.EnvironmentInfo;

@Singleton
public class EnvironmentAdaptor {
  public EnvironmentInfo getEnvironmentInfo(Environment environment) {
    return EnvironmentInfo.builder()
        .id(environment.getUuid())
        .name(environment.getName())
        .description(environment.getDescription())
        .environmentType(environment.getEnvironmentType().name())
        .build();
  }
}
