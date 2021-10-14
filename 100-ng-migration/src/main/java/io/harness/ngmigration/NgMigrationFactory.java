package io.harness.ngmigration;

import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NgMigration;

import com.google.inject.Inject;

public class NgMigrationFactory {
  @Inject PipelineMigrationService pipelineMigrationService;
  @Inject WorkflowMigrationService workflowMigrationService;
  @Inject ConnectorMigrationService connectorMigrationService;
  @Inject ServiceMigrationService serviceMigrationService;
  @Inject ArtifactStreamMigrationService artifactStreamMigrationService;
  @Inject SecretMigrationService secretMigrationService;
  @Inject SecretManagerMigrationService secretManagerMigrationService;

  public NgMigration getMethod(NGMigrationEntityType type) {
    switch (type) {
      case PIPELINE:
        return pipelineMigrationService;
      case WORKFLOW:
        return workflowMigrationService;
      case CONNECTOR:
        return connectorMigrationService;
      case SERVICE:
        return serviceMigrationService;
      case ARTIFACT_STREAM:
        return artifactStreamMigrationService;
      case SECRET:
        return secretMigrationService;
      case SECRET_MANAGER:
        return secretManagerMigrationService;
      default:
        throw new IllegalStateException();
    }
  }
}
