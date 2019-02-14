package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.delegate.beans.ScriptType;
import io.harness.delegate.beans.ShellScriptApprovalTaskParameters;
import io.harness.delegate.task.protocol.AwsElbListener;
import io.harness.serializer.KryoRegistrar;

public class DelegateTasksKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(ShellScriptApprovalTaskParameters.class, 20001);
    kryo.register(ScriptType.class, 5253);
    kryo.register(AwsElbListener.class, 5600);
  }
}
