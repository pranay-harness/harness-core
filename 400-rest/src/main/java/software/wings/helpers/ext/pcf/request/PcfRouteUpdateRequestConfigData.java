package software.wings.helpers.ext.pcf.request;

import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfRouteUpdateRequestConfigData {
  private String newApplicatiaonName;
  private List<PcfAppSetupTimeDetails> existingApplicationDetails;
  private List<String> existingApplicationNames;
  private List<String> tempRoutes;
  private List<String> finalRoutes;
  private boolean isRollback;
  private boolean isStandardBlueGreen;
  private boolean downsizeOldApplication;
  private boolean isMapRoutesOperation;
  private boolean skipRollback;
}
