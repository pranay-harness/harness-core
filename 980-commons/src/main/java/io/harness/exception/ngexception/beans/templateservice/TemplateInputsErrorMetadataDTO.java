package io.harness.exception.ngexception.beans.templateservice;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ngexception.ErrorMetadataDTO;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CDC)
public class TemplateInputsErrorMetadataDTO implements ErrorMetadataDTO {
  String errorYaml;
  Map<String, TemplateInputsErrorDTO> errorMap;

  @Builder
  public TemplateInputsErrorMetadataDTO(String errorYaml, Map<String, TemplateInputsErrorDTO> errorMap) {
    this.errorMap = errorMap;
    this.errorYaml = errorYaml;
  }

  @Override
  public String getType() {
    return "TemplateInputsErrorMetadata";
  }
}
