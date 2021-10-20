package io.harness.ngmigration;

import io.harness.ngmigration.service.NgMigration;

import software.wings.ngmigration.NGMigrationEntityType;

import com.google.inject.Inject;

public class NgMigrationFactory {
  @Inject PipelineMigrationService pipelineMigrationService;
  @Inject WorkflowMigrationService workflowMigrationService;
  @Inject ConnectorMigrationService connectorMigrationService;
  @Inject ServiceMigrationService serviceMigrationService;
  @Inject ArtifactStreamMigrationService artifactStreamMigrationService;
  @Inject SecretMigrationService secretMigrationService;
  @Inject SecretManagerMigrationService secretManagerMigrationService;
  @Inject EnvironmentMigrationService environmentMigrationService;
  @Inject InfraMigrationService infraMigrationService;

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
      case ENVIRONMENT:
        return environmentMigrationService;
      case INFRA:
        return infraMigrationService;
      default:
        throw new IllegalStateException();
    }
  }
}
