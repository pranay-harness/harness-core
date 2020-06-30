package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.CVHistogram;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.SplunkSampleResponse;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.ThirdPartyApiResponseStatus;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.core.services.entities.MetricPack.MetricDefinition;
import io.harness.cvng.models.VerificationType;
import io.harness.serializer.KryoRegistrar;

@OwnedBy(CV)
public class CVNextGenCommonsBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(MetricPack.class, 9000);
    kryo.register(MetricDefinition.class, 9001);
    kryo.register(DataSourceType.class, 9002);
    kryo.register(TimeSeriesMetricType.class, 9003);
    kryo.register(AppDynamicsDataCollectionInfo.class, 9007);
    kryo.register(VerificationType.class, 9008);
    kryo.register(CVHistogram.class, 9009);
    kryo.register(CVHistogram.Bar.class, 9010);
    kryo.register(AppdynamicsValidationResponse.class, 9011);
    kryo.register(AppdynamicsValidationResponse.AppdynamicsMetricValueValidationResponse.class, 9012);
    kryo.register(ThirdPartyApiResponseStatus.class, 9013);
    kryo.register(SplunkSavedSearch.class, 9014);
    kryo.register(SplunkSampleResponse.class, 9015);
  }
}
