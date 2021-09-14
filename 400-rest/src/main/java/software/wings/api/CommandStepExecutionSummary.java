/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.beans.Tag;
import software.wings.beans.command.CodeDeployParams;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.sm.StepExecutionSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by rishi on 4/4/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class CommandStepExecutionSummary extends StepExecutionSummary {
  private String serviceId;
  private List<ContainerServiceData> newInstanceData = new ArrayList<>();
  private List<ContainerServiceData> oldInstanceData = new ArrayList<>();
  private String namespace;
  private String controllerNamePrefix;
  // Following 3 fields are required while Daemon ECS service rollback
  private String previousEcsServiceSnapshotJson;
  private String ecsServiceArn;
  private String ecsTaskDefintion;
  private List<AwsAutoScalarConfig> previousAwsAutoScalarConfigs;
  private String clusterName;
  private CodeDeployParams codeDeployParams;
  private CodeDeployParams oldCodeDeployParams;
  private String codeDeployDeploymentId;
  private String releaseName;

  // For Aws Lambda
  private List<String> aliases;
  private List<Tag> tags;
  private List<FunctionMeta> lambdaFunctionMetaList;
  private String artifactId;
}
