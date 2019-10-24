package software.wings.service.impl.aws.model.response;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import lombok.Builder;
import lombok.Data;
import software.wings.service.impl.aws.model.AwsResponse;

import java.util.List;

@Data
public class AwsCloudWatchStatisticsResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private String label;
  private List<Datapoint> datapoints;

  @Builder
  public AwsCloudWatchStatisticsResponse(DelegateMetaInfo delegateMetaInfo, ExecutionStatus executionStatus,
      String errorMessage, String label, List<Datapoint> datapoints) {
    this.delegateMetaInfo = delegateMetaInfo;
    this.executionStatus = executionStatus;
    this.errorMessage = errorMessage;
    this.label = label;
    this.datapoints = datapoints;
  }
}
