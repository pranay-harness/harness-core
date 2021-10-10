package software.wings.helpers.ext.azure;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._970_API_SERVICES_BEANS)
public class TagValue {
  private String id;
  private String tagValue;
  private TagCount count;
}
