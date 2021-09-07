package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ChecksumType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PL)
public class FileServiceCommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ChecksumType.class, 5065);
  }
}
