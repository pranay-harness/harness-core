package software.wings.annotation;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.Encryptable;
import software.wings.settings.SettingValue.SettingVariableTypes;

public interface EncryptableSetting extends Encryptable { @SchemaIgnore SettingVariableTypes getSettingType(); }
