package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsRoute53HostedZoneData {
  private String hostedZoneId;
  private String hostedZoneName;
}