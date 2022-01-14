package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.SecretSpec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = SecretTextSpecDTO.class, name = "SecretText")
      , @JsonSubTypes.Type(value = SecretFileSpecDTO.class, name = "SecretFile"),
          @JsonSubTypes.Type(value = SSHKeySpecDTO.class, name = "SSHKey"),
    })
@Schema(name = "SecretSpec", description = "This has details of the Secret defined in Harness.")
public abstract class SecretSpecDTO {
  public abstract Optional<String> getErrorMessageForInvalidYaml();

  public abstract SecretSpec toEntity();
}
