package software.wings.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FileUploadLimit {
  @JsonProperty private long appContainerLimit = 1000000000L;
  @JsonProperty private long configFileLimit = 100000000L;
  @JsonProperty private long hostUploadLimit = 100000000L;
  @JsonProperty private long profileResultLimit = 100000000L;
}
