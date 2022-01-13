/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.aggregation.deployment;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLDataType;

@OwnedBy(HarnessTeam.DX)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLDeploymentFilterType {
  Application(DeploymentMetaDataFields.APPID),
  Service(DeploymentMetaDataFields.SERVICEID),
  DeploymentType(DeploymentMetaDataFields.DEPLOYMENT_TYPE),
  Environment(DeploymentMetaDataFields.ENVID),
  EnvironmentType(DeploymentMetaDataFields.ENVTYPE),
  CloudProvider(DeploymentMetaDataFields.CLOUDPROVIDERID),
  Status(DeploymentMetaDataFields.STATUS),
  EndTime(DeploymentMetaDataFields.ENDTIME),
  StartTime(DeploymentMetaDataFields.STARTTIME),
  Duration(DeploymentMetaDataFields.DURATION),
  RollbackDuration(DeploymentMetaDataFields.ROLLBACK_DURATION),
  TriggeredBy(DeploymentMetaDataFields.TRIGGERED_BY),
  Trigger(DeploymentMetaDataFields.TRIGGER_ID),
  Workflow(DeploymentMetaDataFields.WORKFLOWID),
  WorkflowType(DeploymentMetaDataFields.WORKFLOW_TYPE),
  OrchestrationWorkflowType(DeploymentMetaDataFields.ORCHESTRATION_WORKFLOW_TYPE),
  Pipeline(DeploymentMetaDataFields.PIPELINEID),
  Tag(null),
  Tags(DeploymentMetaDataFields.TAGS);

  private QLDataType dataType;
  private DeploymentMetaDataFields metaDataFields;
  QLDeploymentFilterType() {}

  QLDeploymentFilterType(DeploymentMetaDataFields metaDataFields) {
    this.metaDataFields = metaDataFields;
  }

  public DeploymentMetaDataFields getMetaDataFields() {
    return metaDataFields;
  }
}
