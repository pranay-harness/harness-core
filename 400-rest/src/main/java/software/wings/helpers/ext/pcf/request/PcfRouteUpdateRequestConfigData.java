package software.wings.helpers.ext.pcf.request;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
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
