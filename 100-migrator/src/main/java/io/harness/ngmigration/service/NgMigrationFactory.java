/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

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
