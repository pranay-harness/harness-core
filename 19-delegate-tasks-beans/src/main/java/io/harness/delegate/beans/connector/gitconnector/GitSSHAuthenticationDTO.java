package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Ssh")
public class GitSSHAuthenticationDTO extends GitAuthenticationDTO {
  @JsonProperty("type") GitConnectionType gitConnectionType;
  @NotBlank String url;
  @Encrypted(fieldName = "sshKey", isReference = true) char[] sshKey;
  @JsonProperty("sshKeyReference") String encryptedSshKey;
  String branchName;
}
