/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionInterruptType;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutput;
import io.harness.beans.terraform.TerraformPlanParam;
import io.harness.context.ContextElementType;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO.ApiCallLogDTOField;
import io.harness.serializer.KryoRegistrar;

import software.wings.api.CloudProviderType;
import software.wings.api.ContainerServiceData;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.GitFileConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Log;
import software.wings.beans.PhaseStepType;
import software.wings.beans.VMSSAuthType;
import software.wings.beans.VMSSDeploymentType;
import software.wings.beans.VariableType;
import software.wings.beans.artifact.ArtifactStreamSummary;
import software.wings.beans.artifact.ArtifactSummary;
import software.wings.beans.trigger.WebhookSource;
import software.wings.metrics.MetricType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.sm.ExecutionInterruptEffect;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateTypeScope;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDP)
public class CgOrchestrationBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ContextElementType.class, 4004);
    kryo.register(ExecutionStatus.class, 5136);
    kryo.register(GitFileConfig.class, 5472);
    kryo.register(LicenseInfo.class, 5511);
    kryo.register(ThirdPartyApiCallLog.class, 5377);
    kryo.register(ArtifactStreamSummary.class, 7202);
    kryo.register(ArtifactSummary.class, 8127);
    kryo.register(Log.class, 71102);
    kryo.register(ThirdPartyApiCallLog.ThirdPartyApiCallField.class, 71100);
    kryo.register(ThirdPartyApiCallLog.FieldType.class, 71101);

    // Put promoted classes here and do not change the id
    kryo.register(SweepingOutput.class, 3101);
    kryo.register(ExecutionInterruptType.class, 4000);
    kryo.register(ContainerServiceData.class, 5157);
    kryo.register(ExecutionDataValue.class, 5368);
    kryo.register(CountsByStatuses.class, 4008);
    kryo.register(MetricType.class, 5313);
    kryo.register(EntityType.class, 5360);
    kryo.register(ErrorStrategy.class, 4005);
    kryo.register(ExecutionStrategy.class, 4002);
    kryo.register(PhaseStepType.class, 5026);
    kryo.register(VariableType.class, 5379);
    kryo.register(ExecutionInterruptEffect.class, 5236);
    kryo.register(PipelineSummary.class, 5142);
    kryo.register(StateTypeScope.class, 5144);
    kryo.register(WebhookSource.class, 8551);
    kryo.register(ApiCallLogDTO.class, 9048);
    kryo.register(ApiCallLogDTOField.class, 9049);
    kryo.register(ApiCallLogDTO.FieldType.class, 9050);
    kryo.register(AwsInstanceFilter.class, 40092);
    kryo.register(VMSSDeploymentType.class, 400124);
    kryo.register(VMSSAuthType.class, 400127);
    kryo.register(AmiDeploymentType.class, 400125);
    kryo.register(CloudProviderType.class, 400126);
    kryo.register(TerraformPlanParam.class, 7458);
  }
}
