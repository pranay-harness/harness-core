package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.AppdynamicsValidationResponse.AppdynamicsMetricValueValidationResponse;
import io.harness.cvng.beans.CVHistogram;
import io.harness.cvng.models.ThirdPartyApiResponseStatus;
import io.harness.serializer.KryoRegistrar;

@OwnedBy(CV)
public class CVNextGenRestBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(CVHistogram.class, 9500);
    kryo.register(CVHistogram.Bar.class, 9501);
    kryo.register(AppdynamicsValidationResponse.class, 9502);
    kryo.register(AppdynamicsMetricValueValidationResponse.class, 9503);
    kryo.register(ThirdPartyApiResponseStatus.class, 9504);
  }
}