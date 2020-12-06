package software.wings.service.impl.aws.model.request;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsRequest;

import com.amazonaws.services.cloudwatch.model.MetricDataQuery;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCloudWatchMetricDataRequest extends AwsRequest {
  private String region;
  private Date startTime;
  private Date endTime;
  private Collection<MetricDataQuery> metricDataQueries;

  @Builder
  private AwsCloudWatchMetricDataRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, Date startTime, Date endTime, Collection<MetricDataQuery> metricDataQueries) {
    super(awsConfig, encryptionDetails);
    this.region = region;
    this.startTime = startTime;
    this.endTime = endTime;
    this.metricDataQueries = metricDataQueries;
  }
}
