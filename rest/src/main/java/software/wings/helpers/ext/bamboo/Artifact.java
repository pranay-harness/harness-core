package software.wings.helpers.ext.bamboo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 9/6/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Artifact {
  private String name;
  private String link;
  private String producerJobKey;
}
