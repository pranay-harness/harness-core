package io.harness.exception.ngexception;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SampleErrorMetadataDTO implements ErrorMetadataDTO {
  private Map<String, String> sampleMap;

  @Override
  public String getType() {
    return "Sample";
  }
}
