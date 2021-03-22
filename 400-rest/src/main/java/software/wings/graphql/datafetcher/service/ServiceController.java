package software.wings.graphql.datafetcher.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Service;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.QLService.QLServiceBuilder;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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

  public static QLService buildQLService(Service service) {
    QLServiceBuilder builder = QLService.builder();
    populateService(service, builder);
    return builder.build();
  }
}
