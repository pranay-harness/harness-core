package software.wings.graphql.datafetcher.service;

import lombok.experimental.UtilityClass;
import software.wings.beans.Service;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLService.QLServiceBuilder;

@UtilityClass
public class ServiceController {
  public static void populateService(Service service, QLServiceBuilder builder) {
    builder.id(service.getUuid())
        .name(service.getName())
        .applicationId(service.getAppId())
        .description(service.getDescription())
        .artifactType(service.getArtifactType())
        .deploymentType(service.getDeploymentType())
        .createdAt(service.getCreatedAt())
        .createdBy(UserController.populateUser(service.getCreatedBy()));
  }
}
