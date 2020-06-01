package software.wings.helpers.ext.gcb.models;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;

import java.util.List;

@OwnedBy(CDC)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Data
public class GcbArtifactObjects {
  private String location;
  private List<String> paths;
  private TimeSpan timing;
}
