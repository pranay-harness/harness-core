package io.harness.delegate.beans.ci.pod;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
public class SecretVariableDetails {
  @NotNull SecretVariableDTO secretVariableDTO;
  @NotNull List<EncryptedDataDetail> encryptedDataDetailList;
}
