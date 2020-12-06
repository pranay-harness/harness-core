package software.wings.beans.infrastructure.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSInstanceInfo extends InstanceInfo {
  private String vmssId;
  private String azureVMId;
  private String host;
  private String state;
  private String instanceType;
}
