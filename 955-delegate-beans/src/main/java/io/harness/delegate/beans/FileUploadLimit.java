package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@OwnedBy(HarnessTeam.DEL)
public class FileUploadLimit {
  @JsonProperty private long appContainerLimit = 1000000000L;
  @JsonProperty private long configFileLimit = 100000000L;
  @JsonProperty private long hostUploadLimit = 100000000L;
  @JsonProperty private long profileResultLimit = 100000000L;
  @JsonProperty private long encryptedFileLimit = 10L * 1024 * 1024 /*10 MB */;
  @JsonProperty private long credentialFileLimit = 20L * 1024; /*20 KB */
  @JsonProperty private long commandUploadLimit = 10L * 1024 * 1024 /*10 MB */;
}
