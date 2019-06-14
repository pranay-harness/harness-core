package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.SecretDetail;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.SSHConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.WinRMExecutionCapability;
import io.harness.delegate.command.CommandExecutionData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.delegate.task.shell.ShellScriptApprovalTaskParameters;
import io.harness.serializer.KryoRegistrar;

public class DelegateTasksKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ShellScriptApprovalTaskParameters.class, 20001);
    kryo.register(HttpTaskParameters.class, 20002);
    kryo.register(ScriptType.class, 5253);
    kryo.register(AwsElbListener.class, 5600);
    kryo.register(CommandExecutionData.class, 5035);
    kryo.register(CommandExecutionResult.class, 5036);
    kryo.register(CommandExecutionStatus.class, 5037);
    kryo.register(DelegateScripts.class, 5002);
    kryo.register(DelegateConfiguration.class, 5469);
    kryo.register(SecretDetail.class, 19001);
    kryo.register(TaskData.class, 19002);
    kryo.register(HttpConnectionExecutionCapability.class, 19003);
    kryo.register(CapabilityType.class, 19004);
    kryo.register(DelegateMetaInfo.class, 5372);
    kryo.register(DelegateTaskNotifyResponseData.class, 5373);
    kryo.register(SSHConnectionExecutionCapability.class, 19005);
    kryo.register(WinRMExecutionCapability.class, 19006);
  }
}
