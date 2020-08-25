package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.MetricPackDTO.MetricDefinitionDTO;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
import io.harness.cvng.beans.SplunkValidationResponse.SplunkSampleResponse;
import io.harness.cvng.beans.ThirdPartyApiResponseStatus;
import io.harness.cvng.beans.TimeSeriesCustomThresholdActions;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdComparisonType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.cvng.beans.TimeSeriesThresholdDTO;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.cvng.models.VerificationType;
import io.harness.serializer.KryoRegistrar;

@OwnedBy(CV)
public class CvNextGenCommonsBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(MetricDefinitionDTO.class, 9001);
    kryo.register(DataSourceType.class, 9002);
    kryo.register(TimeSeriesMetricType.class, 9003);
    kryo.register(CVMonitoringCategory.class, 9004);
    kryo.register(AppDynamicsDataCollectionInfo.class, 9007);
    kryo.register(VerificationType.class, 9008);
    kryo.register(SplunkValidationResponse.Histogram.class, 9009);
    kryo.register(SplunkValidationResponse.Histogram.Bar.class, 9010);
    kryo.register(AppdynamicsValidationResponse.class, 9011);
    kryo.register(AppdynamicsValidationResponse.AppdynamicsMetricValueValidationResponse.class, 9012);
    kryo.register(ThirdPartyApiResponseStatus.class, 9013);
    kryo.register(SplunkSavedSearch.class, 9014);
    kryo.register(SplunkSampleResponse.class, 9015);
    kryo.register(MetricPackDTO.class, 9016);
    kryo.register(SplunkValidationResponse.class, 9017);
    kryo.register(SplunkValidationResponse.SampleLog.class, 9018);
    kryo.register(DataCollectionConnectorBundle.class, 9019);
    kryo.register(AppDynamicsApplication.class, 9020);
    kryo.register(AppDynamicsTier.class, 9021);
    kryo.register(TimeSeriesThresholdDTO.class, 9022);
    kryo.register(TimeSeriesThresholdActionType.class, 9023);
    kryo.register(TimeSeriesThresholdCriteria.class, 9024);
    kryo.register(TimeSeriesThresholdComparisonType.class, 9025);
    kryo.register(TimeSeriesThresholdType.class, 9026);
    kryo.register(TimeSeriesCustomThresholdActions.class, 9027);
  }
}
