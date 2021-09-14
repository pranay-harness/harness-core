/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.ecs.request;

import static software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType.DEPLOY_ROLLBACK_DATA_FETCH;

import software.wings.beans.AwsConfig;
import software.wings.beans.command.EcsResizeParams;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsDeployRollbackDataFetchRequest extends EcsCommandRequest {
  private EcsResizeParams ecsResizeParams;

  @Builder
  public EcsDeployRollbackDataFetchRequest(String accountId, String appId, String commandName, String activityId,
      String region, String cluster, AwsConfig awsConfig, EcsResizeParams ecsResizeParams,
      boolean timeoutErrorSupported) {
    super(accountId, appId, commandName, activityId, region, cluster, awsConfig, DEPLOY_ROLLBACK_DATA_FETCH,
        timeoutErrorSupported);
    this.ecsResizeParams = ecsResizeParams;
  }
}
