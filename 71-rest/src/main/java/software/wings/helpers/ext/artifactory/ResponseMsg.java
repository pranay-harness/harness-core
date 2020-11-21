package software.wings.helpers.ext.artifactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ResponseMsg {
  private List<Error> errors;
  @Data
  public static class Error {
    String status;
    String message;
  }
}
