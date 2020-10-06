package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.beans.InputSetValidator;
import io.harness.beans.ParameterField;
import io.harness.serializer.KryoRegistrar;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.variables.NumberNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;

public class YamlKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ParameterField.class, 35001);
    kryo.register(InputSetValidator.class, 35002);
    kryo.register(StageElement.class, 35003);
    kryo.register(ExecutionElement.class, 35004);
    kryo.register(StepElement.class, 35005);
    kryo.register(StringNGVariable.class, 35006);
    kryo.register(NumberNGVariable.class, 35007);
  }
}
