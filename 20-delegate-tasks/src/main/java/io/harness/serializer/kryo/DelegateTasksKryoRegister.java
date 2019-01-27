package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.delegates.beans.ScriptType;
import io.harness.delegates.beans.ShellScriptApprovalTaskParameters;
import io.harness.serializer.KryoRegistrar;

public class DelegateTasksKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(ShellScriptApprovalTaskParameters.class, 20001);
    kryo.register(ScriptType.class, 5253);
  }
}
