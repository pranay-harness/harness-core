package software.wings.beans.delegation;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

@Builder
@Getter
public class ScriptParameters {
  public final static String CommandUnit = "Execute";

  @Setter private String accountId;
  private final String appId;
  private final String activityId;

  private final String host;
  private final List<EncryptedDataDetail> keyEncryptedDataDetails;

  private final String script;
}
