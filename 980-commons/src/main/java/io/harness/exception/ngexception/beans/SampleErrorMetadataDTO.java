package io.harness.exception.ngexception.beans;

import io.harness.exception.ngexception.ErrorMetadataDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "SampleErrorMetadata", description = "This has error messages.")
public class SampleErrorMetadataDTO implements ErrorMetadataDTO {
  private Map<String, String> sampleMap;

  @Override
  public String getType() {
    return "Sample";
  }
}
