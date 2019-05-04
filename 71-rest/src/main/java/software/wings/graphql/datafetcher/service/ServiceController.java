package software.wings.graphql.datafetcher.service;

import software.wings.beans.Service;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLService.QLServiceBuilder;

public class ServiceController {
  public static void populateService(Service service, QLServiceBuilder builder) {
    builder.id(service.getUuid())
        .name(service.getName())
        .description(service.getDescription())
        .artifactType(service.getArtifactType())
        .deploymentType(service.getDeploymentType())
        .createdAt(GraphQLDateTimeScalar.convert(service.getCreatedAt()))
        .createdBy(UserController.populateUser(service.getCreatedBy()));
  }
}
