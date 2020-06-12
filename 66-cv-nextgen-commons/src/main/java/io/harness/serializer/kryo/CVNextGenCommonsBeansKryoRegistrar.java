package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.core.services.entities.MetricPack.MetricDefinition;
import io.harness.cvng.core.services.entities.TimeSeriesRecord;
import io.harness.cvng.core.services.entities.TimeSeriesRecord.TimeSeriesGroupValue;
import io.harness.cvng.core.services.entities.TimeSeriesRecord.TimeSeriesValue;
import io.harness.serializer.KryoRegistrar;

@OwnedBy(CV)
public class CVNextGenCommonsBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(MetricPack.class, 9000);
    kryo.register(MetricDefinition.class, 9001);
    kryo.register(TimeSeriesRecord.class, 9002);
    kryo.register(TimeSeriesGroupValue.class, 9003);
    kryo.register(TimeSeriesValue.class, 9004);
    kryo.register(DataSourceType.class, 9005);
    kryo.register(TimeSeriesMetricType.class, 9006);
  }
}