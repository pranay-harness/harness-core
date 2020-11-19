package io.harness.pms.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;
import io.harness.pms.execution.ExecutionMode;
import io.harness.pms.execution.Status;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.refobjects.RefObject;
import io.harness.pms.refobjects.RefType;
import io.harness.pms.serializer.kryo.serializers.LevelKryoSerializer;
import io.harness.pms.serializer.kryo.serializers.RefTypeKryoSerializer;
import io.harness.pms.serializer.kryo.serializers.StepTypeKryoSerializer;
import io.harness.pms.steps.SkipType;
import io.harness.pms.steps.StepType;
import io.harness.serializer.KryoRegistrar;

public class PmsContractsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(Ambiance.class, 2601);
    kryo.register(Level.class, new LevelKryoSerializer(), 2602);
    kryo.register(ExecutionMode.class, 2603);
    kryo.register(Status.class, 2604);
    kryo.register(AdviserType.class, 2605);
    kryo.register(AdviserObtainment.class, 2606);
    kryo.register(SkipType.class, 2607);
    kryo.register(FacilitatorType.class, 2608);
    kryo.register(StepType.class, new StepTypeKryoSerializer(), 2611);
    kryo.register(RefType.class, new RefTypeKryoSerializer(), 2609);
    kryo.register(RefObject.class, 2610);
  }
}