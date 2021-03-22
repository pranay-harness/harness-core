package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.CVDataCollectionInfo;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.K8ActivityDataCollectionInfo;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.MetricPackDTO.MetricDefinitionDTO;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
import io.harness.cvng.beans.SplunkValidationResponse.SplunkSampleResponse;
import io.harness.cvng.beans.StackdriverDataCollectionInfo;
import io.harness.cvng.beans.ThirdPartyApiResponseStatus;
import io.harness.cvng.beans.TimeSeriesCustomThresholdActions;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdComparisonType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.cvng.beans.TimeSeriesThresholdDTO;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsDataCollectionRequest;
import io.harness.cvng.beans.appd.AppDynamicsFetchAppRequest;
import io.harness.cvng.beans.appd.AppDynamicsFetchTiersRequest;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO.ApiCallLogDTOField;
import io.harness.cvng.beans.splunk.SplunkDataCollectionRequest;
import io.harness.cvng.beans.splunk.SplunkSavedSearchRequest;
import io.harness.cvng.beans.stackdriver.StackDriverMetricDefinition;
import io.harness.cvng.beans.stackdriver.StackdriverDashboardDetailsRequest;
import io.harness.cvng.beans.stackdriver.StackdriverDashboardRequest;
import io.harness.cvng.beans.stackdriver.StackdriverSampleDataRequest;
import io.harness.cvng.models.VerificationType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

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
    kryo.register(DataCollectionType.class, 9028);
    kryo.register(CVDataCollectionInfo.class, 9029);
    kryo.register(K8ActivityDataCollectionInfo.class, 9030);
    kryo.register(KubernetesActivitySourceDTO.class, 9031);
    kryo.register(KubernetesActivitySourceConfig.class, 9032);
    kryo.register(SplunkDataCollectionRequest.class, 9034);
    kryo.register(SplunkSavedSearchRequest.class, 9035);
    kryo.register(ActivityStatusDTO.class, 9037);
    kryo.register(ActivityVerificationStatus.class, 9038);
    kryo.register(StackdriverDashboardRequest.class, 9039);
    kryo.register(StackdriverDashboardDetailsRequest.class, 9040);
    kryo.register(StackdriverSampleDataRequest.class, 9041);
    kryo.register(StackDriverMetricDefinition.class, 9042);
    kryo.register(StackDriverMetricDefinition.Aggregation.class, 9043);
    kryo.register(StackdriverDataCollectionInfo.class, 9044);
    kryo.register(AppDynamicsDataCollectionRequest.class, 9045);
    kryo.register(AppDynamicsFetchAppRequest.class, 9046);
    kryo.register(AppDynamicsFetchTiersRequest.class, 9047);

    kryo.register(ApiCallLogDTO.class, 9048);
    kryo.register(ApiCallLogDTOField.class, 9049);
    kryo.register(ApiCallLogDTO.FieldType.class, 9050);
  }
}
