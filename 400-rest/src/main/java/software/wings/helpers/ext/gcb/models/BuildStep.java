package software.wings.helpers.ext.gcb.models;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Data;

@OwnedBy(CDC)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Data
@TargetModule(HarnessModule._960_API_SERVICES)
public class BuildStep {
  private String name;
  private List<String> env;
  private List<String> args;
  private String dir;
  private String id;
  private List<String> waitFor;
  private String entrypoint;
  private List<String> secretEnv;
  private List<Volume> volumes;
  private TimeSpan timing;
  private TimeSpan pullTiming;
  private GcbBuildStatus status;
}
