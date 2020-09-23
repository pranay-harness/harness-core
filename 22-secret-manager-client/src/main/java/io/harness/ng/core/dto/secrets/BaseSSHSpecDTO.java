package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.ng.core.models.BaseSSHSpec;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "authScheme", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = SSHConfigDTO.class, name = "SSH")
      , @JsonSubTypes.Type(value = KerberosConfigDTO.class, name = "Kerberos"),
    })
public abstract class BaseSSHSpecDTO {
  public abstract BaseSSHSpec toEntity();
}
