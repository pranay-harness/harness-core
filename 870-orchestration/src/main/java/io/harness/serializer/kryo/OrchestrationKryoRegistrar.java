package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.interrupts.AbortInterruptCallback;
import io.harness.engine.interrupts.callback.FailureInterruptCallback;
import io.harness.engine.interrupts.handlers.AbortAllInterruptCallback;
import io.harness.engine.pms.EngineAdviseCallback;
import io.harness.engine.pms.EngineFacilitationCallback;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.engine.resume.EngineResumeAllCallback;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.engine.resume.EngineWaitRetryCallback;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class OrchestrationKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(EngineResumeAllCallback.class, 87001);
    kryo.register(EngineResumeCallback.class, 87002);
    kryo.register(EngineWaitResumeCallback.class, 87003);
    kryo.register(EngineWaitRetryCallback.class, 87004);
    kryo.register(EngineFacilitationCallback.class, 87005);
    kryo.register(EngineAdviseCallback.class, 87006);
    kryo.register(EngineProgressCallback.class, 87007);
    kryo.register(AbortInterruptCallback.class, 87008);
    kryo.register(AbortAllInterruptCallback.class, 87009);
    kryo.register(FailureInterruptCallback.class, 87010);
  }
}
