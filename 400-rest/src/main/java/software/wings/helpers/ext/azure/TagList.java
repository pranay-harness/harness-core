package software.wings.helpers.ext.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class TagList {
  private List<TagDetails> details;
}
