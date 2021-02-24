package io.harness.serializer.kryo;

import io.harness.pms.execution.beans.RepresentationStrategy;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.validation.InputSetValidator;
import io.harness.pms.yaml.validation.InputSetValidatorType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class PmsCommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // keeping ids same
    kryo.register(ParameterField.class, 35001);
    kryo.register(InputSetValidator.class, 35002);
    kryo.register(InputSetValidatorType.class, 35008);
    kryo.register(RepresentationStrategy.class, 35012);
  }
}
