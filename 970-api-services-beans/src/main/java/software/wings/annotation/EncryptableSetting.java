package software.wings.annotation;

import io.harness.beans.Encryptable;

import software.wings.settings.SettingVariableTypes;

import com.github.reinert.jjschema.SchemaIgnore;

public interface EncryptableSetting extends Encryptable {
  @SchemaIgnore SettingVariableTypes getSettingType();
}
