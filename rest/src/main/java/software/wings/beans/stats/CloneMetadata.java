package software.wings.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.Environment;
import software.wings.beans.Workflow;

import java.util.Map;

/**
 * Created by sgurubelli on 9/8/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloneMetadata {
  String targetAppId;
  Map<String, String> serviceMapping;
  Workflow workflow;
  Environment environment;
}
