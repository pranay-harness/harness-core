package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AzureLogAnalyticsConnectionDetails {
  private String clientId;
  private String clientSecret;
  private String tenantId;
}
