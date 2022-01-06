/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatusResponseData;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.RepairActionCode;
import io.harness.beans.ShellScriptProvisionOutputVariables;
import io.harness.beans.WorkflowType;
import io.harness.serializer.KryoRegistrar;

import software.wings.api.ecs.EcsBGSetupData;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.TailFilePatternEntry;
import software.wings.sm.BarrierStatusData;
import software.wings.utils.FileType;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class CgOrchestrationKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(OrchestrationWorkflowType.class, 5148);
    kryo.register(WorkflowType.class, 5025);
    kryo.register(DelegateTask.Status.class, 5004);
    kryo.register(DelegateTask.class, 5003);
    kryo.register(FileType.class, 5119);

    kryo.register(ExecutionStatusResponseData.class, 3102);
    kryo.register(RepairActionCode.class, 2528);
    kryo.register(ShellScriptProvisionOutputVariables.class, 40021);

    kryo.register(BarrierStatusData.class, 7277);

    // Put promoted classes here and do not change the id
    kryo.register(CommandType.class, 5038);
    kryo.register(TailFilePatternEntry.class, 5059);
    kryo.register(EcsBGSetupData.class, 5611);
  }
}
