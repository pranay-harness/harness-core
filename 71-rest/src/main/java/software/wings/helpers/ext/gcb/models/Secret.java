package software.wings.helpers.ext.gcb.models;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;

import java.util.Map;

@OwnedBy(CDC)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Secret {
  private String kmsKeyName;
  private Map<String, String> secretEnv;
}
