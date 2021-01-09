package io.serializer.kryo;

import io.harness.encryption.SecretRefData;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class NGCommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(NGTag.class, 22001);
    kryo.register(BaseNGAccess.class, 54324);
    kryo.register(SecretRefData.class, 3003);
    kryo.register(ErrorDetail.class, 54325);
  }
}
