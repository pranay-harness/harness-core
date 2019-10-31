package software.wings.service.impl.aws.model.response;

import com.amazonaws.services.cloudwatch.model.MetricDataResult;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import lombok.Builder;
import lombok.Data;
import software.wings.service.impl.aws.model.AwsResponse;

import java.util.List;

@Data
public class AwsCloudWatchMetricDataResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private List<MetricDataResult> metricDataResults;

  @Builder
  public AwsCloudWatchMetricDataResponse(DelegateMetaInfo delegateMetaInfo, ExecutionStatus executionStatus,
      String errorMessage, List<MetricDataResult> metricDataResults) {
    this.delegateMetaInfo = delegateMetaInfo;
    this.executionStatus = executionStatus;
    this.errorMessage = errorMessage;
    this.metricDataResults = metricDataResults;
  }
}
