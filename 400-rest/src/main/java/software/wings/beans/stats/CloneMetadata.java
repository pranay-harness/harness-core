package software.wings.beans.stats;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Environment;
import software.wings.beans.Workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 9/8/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class CloneMetadata {
  String targetAppId;
  Map<String, String> serviceMapping;
  Workflow workflow;
  Environment environment;
}
