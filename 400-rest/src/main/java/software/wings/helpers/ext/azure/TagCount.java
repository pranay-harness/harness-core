package software.wings.helpers.ext.azure;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class TagCount {
  private String type;
  private int value;
}
