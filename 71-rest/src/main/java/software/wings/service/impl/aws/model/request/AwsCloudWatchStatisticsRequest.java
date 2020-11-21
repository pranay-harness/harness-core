package software.wings.service.impl.aws.model.request;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsRequest;

import com.amazonaws.services.cloudwatch.model.Dimension;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCloudWatchStatisticsRequest extends AwsRequest {
  private String region;
  private String namespace;
  private String metricName;
  private Collection<Dimension> dimensions;
  private Date startTime;
  private Date endTime;
  private Integer period;
  private Collection<String> statistics;
  private Collection<String> extendedStatistics;
  private String unit;

  @Builder
  public AwsCloudWatchStatisticsRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String namespace, String metricName, Collection<Dimension> dimensions, Date startTime, Date endTime,
      Integer period, Collection<String> statistics, Collection<String> extendedStatistics, String unit) {
    super(awsConfig, encryptionDetails);
    this.region = region;
    this.namespace = namespace;
    this.metricName = metricName;
    this.dimensions = dimensions;
    this.startTime = startTime != null ? new Date(startTime.getTime()) : null;
    this.endTime = endTime != null ? new Date(endTime.getTime()) : null;
    this.period = period;
    this.statistics = statistics;
    this.extendedStatistics = extendedStatistics;
    this.unit = unit;
  }
}
