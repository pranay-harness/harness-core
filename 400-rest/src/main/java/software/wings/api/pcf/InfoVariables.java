package software.wings.api.pcf;

import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("infoVariables")
public class InfoVariables implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "pcf";

  private String newAppName;
  private String newAppGuid;
  private List<String> newAppRoutes;

  private String oldAppName;
  private String oldAppGuid;
  private List<String> oldAppRoutes;

  private List<String> finalRoutes;
  private List<String> tempRoutes;

  private String mostRecentInactiveAppVersionName;
  private String mostRecentInactiveAppVersionGuid;
  private Integer mostRecentInactiveAppVersionRunningInstances;

  @Override
  public String getType() {
    return "infoVariables";
  }
}
