package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.BaseSSHSpec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = SSHConfigDTO.class, name = "SSH")
      , @JsonSubTypes.Type(value = KerberosConfigDTO.class, name = "Kerberos"),
    })
@Schema(name = "BaseSSHSpec", description = "This is the SSH specification details as defined in Harness.")
public abstract class BaseSSHSpecDTO {
  public abstract BaseSSHSpec toEntity();
}
