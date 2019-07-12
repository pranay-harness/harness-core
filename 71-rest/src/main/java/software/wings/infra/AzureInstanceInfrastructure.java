package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.beans.AzureTag;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("AZURE_SSH")
@Data
public class AzureInstanceInfrastructure implements CloudProviderInfrastructure {
  private String cloudProviderId;
  private String subscriptionId;
  private String resourceGroup;
  private List<AzureTag> tags = new ArrayList<>();
  private String hostConnectionAttrs;
  private String winRmConnectionAttributes;
  private boolean usePublicDns;
}
