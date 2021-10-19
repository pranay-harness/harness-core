package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.interrupts.AbortInterruptCallback;
import io.harness.engine.interrupts.callback.FailureInterruptCallback;
import io.harness.engine.interrupts.handlers.AbortAllInterruptCallback;
import io.harness.engine.pms.resume.EngineResumeAllCallback;
import io.harness.engine.pms.resume.EngineResumeCallback;
import io.harness.engine.pms.resume.EngineWaitRetryCallback;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class OrchestrationKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(EngineResumeAllCallback.class, 87001);
    kryo.register(EngineResumeCallback.class, 87002);
    kryo.register(EngineWaitRetryCallback.class, 87004);
    kryo.register(EngineProgressCallback.class, 87007);
    kryo.register(AbortInterruptCallback.class, 87008);
    kryo.register(AbortAllInterruptCallback.class, 87009);
    kryo.register(FailureInterruptCallback.class, 87010);
  }
}
