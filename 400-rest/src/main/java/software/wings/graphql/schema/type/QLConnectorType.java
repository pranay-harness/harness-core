/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type;

/**
 * @author rktummala on 07/18/19
 */
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLConnectorType implements QLEnum {
  SMTP,
  JENKINS,
  BAMBOO,
  SPLUNK,
  ELK,
  LOGZ,
  SUMO,
  APP_DYNAMICS,
  NEW_RELIC,
  DYNA_TRACE,
  BUG_SNAG,
  DATA_DOG,
  APM_VERIFICATION,
  PROMETHEUS,
  ELB,
  SLACK,
  DOCKER,
  ECR,
  GCR,
  NEXUS,
  ARTIFACTORY,
  AMAZON_S3,
  GCS,
  GIT,
  SMB,
  JIRA,
  SFTP,
  SERVICENOW,
  HTTP_HELM_REPO,
  AMAZON_S3_HELM_REPO,
  GCS_HELM_REPO;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
