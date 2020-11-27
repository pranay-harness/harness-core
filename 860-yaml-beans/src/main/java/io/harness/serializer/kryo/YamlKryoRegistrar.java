package io.harness.serializer.kryo;

import io.harness.beans.InputSetValidator;
import io.harness.beans.InputSetValidatorType;
import io.harness.beans.ParameterField;
import io.harness.serializer.KryoRegistrar;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.variables.NumberNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import io.harness.yaml.extended.ci.codebase.CodeBaseType;
import io.harness.yaml.extended.ci.codebase.impl.GitHubCodeBase;

import com.esotericsoftware.kryo.Kryo;

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
    kryo.register(InputSetValidatorType.class, 35008);
    kryo.register(ParallelStepElement.class, 35009);
    kryo.register(ParallelStageElement.class, 35010);
    kryo.register(CodeBase.class, 35011);
    kryo.register(CodeBaseType.class, 35012);
    kryo.register(GitHubCodeBase.class, 35013);
  }
}
