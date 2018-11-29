package software.wings.settings.validation;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@Builder
public class ConnectivityValidationDelegateRequest {
  private SettingAttribute settingAttribute;
  private List<EncryptedDataDetail> encryptedDataDetails;
}