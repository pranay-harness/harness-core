package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateProfileParams {
  private String name;
  private String profileId;
  private long profileLastUpdatedAt;
  private String scriptContent;
}
