package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsCrossAccountAttributes {
  private String externalId;
  private String crossAccountRoleArn;
}
