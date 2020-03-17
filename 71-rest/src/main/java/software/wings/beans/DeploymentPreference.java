package software.wings.beans;

import static software.wings.beans.PreferenceType.DEPLOYMENT_PREFERENCE;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Indexed;

import java.util.List;

/**
 * User Preference model
 * @author rathna on 04/25/2018
 */

@Data
@JsonTypeName("DEPLOYMENT_PREFERENCE")
@EqualsAndHashCode(callSuper = false)
public class DeploymentPreference extends Preference {
  List<String> appIds;
  List<String> pipelineIds;
  List<String> workflowIds;
  List<String> serviceIds;
  List<String> envIds;
  List<String> status;
  String startTime;
  String endTime;
  boolean includeIndirectExecutions;
  HarnessTagFilter harnessTagFilter;
  private transient String uiDisplayTagString;

  @SchemaIgnore @Indexed private List<String> keywords;

  public DeploymentPreference() {
    super(DEPLOYMENT_PREFERENCE.name());
  }
}
