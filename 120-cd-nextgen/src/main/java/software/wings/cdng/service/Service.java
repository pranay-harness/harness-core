package software.wings.cdng.service;

import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Service implements Outcome {
  private String identifier;
}
