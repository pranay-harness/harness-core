package io.harness.delegate.beans.azure;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class AzureConfigDelegate {
  private AzureConfigDTO azureConfigDTO;
  private List<EncryptedDataDetail> azureEncryptionDetails;
}
