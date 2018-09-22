package software.wings.beans;

import static software.wings.beans.PreferenceType.DEPLOYMENT_PREFERENCE;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

import java.util.List;

/**
 * User Preference model
 * @author rathna on 04/25/2018
 */

@Data
@JsonTypeName("DEPLOYMENT_PREFERENCE")
public class DeploymentPreference extends Preference {
  List<String> appIds;
  List<String> pipelineIds;
  List<String> workflowIds;
  List<String> serviceIds;
  List<String> envIds;
  List<String> status;
  String startTime;
  String endTime;

  public DeploymentPreference() {
    super(DEPLOYMENT_PREFERENCE.name());
  }
}
