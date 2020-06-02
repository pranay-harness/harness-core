package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.AppdynamicsValidationResponse.AppdynamicsMetricValueValidationResponse;
import io.harness.cvng.beans.CVHistogram;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.core.services.entities.TimeSeriesRecord;
import io.harness.cvng.core.services.entities.TimeSeriesRecord.TimeSeriesGroupValue;
import io.harness.cvng.core.services.entities.TimeSeriesRecord.TimeSeriesValue;
import io.harness.cvng.models.DataSourceType;
import io.harness.cvng.models.ThirdPartyApiResponseStatus;
import io.harness.serializer.KryoRegistrar;

@OwnedBy(CV)
public class CVNextGenCommonsBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(CVHistogram.class, 9000);
    kryo.register(CVHistogram.Bar.class, 9001);
    kryo.register(MetricPack.class, 9002);
    kryo.register(MetricPack.MetricDefinition.class, 9003);
    kryo.register(TimeSeriesRecord.class, 9004);
    kryo.register(TimeSeriesGroupValue.class, 9005);
    kryo.register(TimeSeriesValue.class, 9006);
    kryo.register(DataSourceType.class, 9007);
    kryo.register(AppdynamicsValidationResponse.class, 9008);
    kryo.register(AppdynamicsMetricValueValidationResponse.class, 9009);
    kryo.register(ThirdPartyApiResponseStatus.class, 9010);
  }
}