package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.annotation.EncryptableSetting;

import java.util.List;

/**
 * Created by rsingh on 6/7/18.
 */
@OwnedBy(PL)
@TargetModule(HarnessModule._890_SM_CORE)
public interface ManagerDecryptionService {
  void decrypt(EncryptableSetting object, List<EncryptedDataDetail> encryptedDataDetails);

  void decrypt(
      String accountId, List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetailsList);
}
