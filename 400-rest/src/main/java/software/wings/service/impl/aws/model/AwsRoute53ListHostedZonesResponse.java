package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsRoute53ListHostedZonesResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  List<AwsRoute53HostedZoneData> hostedZones;
}
