package io.harness.cvng.perpetualtask;

import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.settings.SettingValue;

import java.util.List;

@Value
@Builder
public class CVDataCollectionInfo {
  SettingValue settingValue;
  List<EncryptedDataDetail> encryptedDataDetails;
  CVConfig cvConfig;
}
