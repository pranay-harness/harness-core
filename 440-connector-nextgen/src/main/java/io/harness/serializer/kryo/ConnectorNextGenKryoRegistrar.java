package io.harness.serializer.kryo;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class ConnectorNextGenKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ConnectorDTO.class, 26001);
    kryo.register(ConnectorInfoDTO.class, 26002);
  }
}
