package io.harness.security.encryption;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;

import software.wings.annotation.EncryptableSetting;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptableSettingWithEncryptionDetails {
  // This generated UUID is for correlating the decrypted data details with the input details.
  @Default private String detailId = UUIDGenerator.generateUuid();
  private EncryptableSetting encryptableSetting;
  private List<EncryptedDataDetail> encryptedDataDetails;
}
