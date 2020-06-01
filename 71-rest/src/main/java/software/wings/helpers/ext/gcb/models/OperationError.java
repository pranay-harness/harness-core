package software.wings.helpers.ext.gcb.models;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;

@OwnedBy(CDC)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OperationError {
  private String code;
  private String message;
}
