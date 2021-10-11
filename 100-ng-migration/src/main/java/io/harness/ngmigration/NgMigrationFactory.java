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
      default:
        throw new IllegalStateException();
    }
  }
}
