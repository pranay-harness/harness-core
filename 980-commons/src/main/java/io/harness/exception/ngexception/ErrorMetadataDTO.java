package io.harness.exception.ngexception;

import io.harness.exception.ngexception.beans.SampleErrorMetadataDTO;
import io.harness.exception.ngexception.beans.templateservice.TemplateInputsErrorMetadataDTO;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = SampleErrorMetadataDTO.class, name = "Sample")
      , @JsonSubTypes.Type(value = TemplateInputsErrorMetadataDTO.class, name = "TemplateInputsErrorMetadata")
    })
public interface ErrorMetadataDTO {
  String getType();
}
