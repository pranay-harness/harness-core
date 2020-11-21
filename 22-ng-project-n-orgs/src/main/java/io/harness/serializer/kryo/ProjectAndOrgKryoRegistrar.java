package io.harness.serializer.kryo;

import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.SampleEncryptableSettingImplementation;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class ProjectAndOrgKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(Organization.class, 54320);
    kryo.register(Project.class, 54321);
    kryo.register(SampleEncryptableSettingImplementation.class, 54322);
    kryo.register(NGAccessWithEncryptionConsumer.class, 54323);
  }
}
