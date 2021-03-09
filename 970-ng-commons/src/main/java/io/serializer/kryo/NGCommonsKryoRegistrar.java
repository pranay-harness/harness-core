package io.serializer.kryo;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.encryption.SecretRefData;
import io.harness.http.HttpHeaderConfig;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.Status;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class NGCommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(NGTag.class, 22001);
    kryo.register(BaseNGAccess.class, 54324);
    kryo.register(SecretRefData.class, 3003);
    kryo.register(ErrorDetail.class, 54325);
    kryo.register(ConnectorValidationResult.class, 19059);
    kryo.register(ConnectivityStatus.class, 19458);
    kryo.register(ResponseDTO.class, 19459);
    kryo.register(Status.class, 19460);
    kryo.register(ErrorDTO.class, 19461);
    kryo.register(HttpHeaderConfig.class, 19462);
  }
}