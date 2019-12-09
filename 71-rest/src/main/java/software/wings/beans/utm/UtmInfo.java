package software.wings.beans.utm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtmInfo {
  private String utmSource;
  private String utmContent;
  private String utmMedium;
  private String utmTerm;
  private String utmCampaign;
}
