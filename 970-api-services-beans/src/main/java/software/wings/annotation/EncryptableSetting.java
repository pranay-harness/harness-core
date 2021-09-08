package software.wings.annotation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.Encryptable;

import software.wings.settings.SettingVariableTypes;

import com.github.reinert.jjschema.SchemaIgnore;

@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public interface EncryptableSetting extends Encryptable {
  @SchemaIgnore SettingVariableTypes getSettingType();
}
