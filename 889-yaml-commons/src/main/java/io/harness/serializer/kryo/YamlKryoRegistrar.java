package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.async.AsyncResponseCallback;
import io.harness.serializer.KryoRegistrar;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.NumberNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import io.harness.yaml.extended.ci.container.ContainerResource;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PIPELINE)
public class YamlKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(StringNGVariable.class, 35006);
    kryo.register(NumberNGVariable.class, 35007);
    kryo.register(CodeBase.class, 35011);
    kryo.register(ContainerResource.class, 35013);
    kryo.register(ContainerResource.Limits.class, 35014);
    kryo.register(AsyncResponseCallback.class, 88407);
    kryo.register(NGVariableType.class, 88501);
  }
}
