package software.wings.logcontext;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;
import software.wings.beans.SettingAttribute;

public class SettingAttributeLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(SettingAttribute.class);

  public SettingAttributeLogContext(String settingId, OverrideBehavior behavior) {
    super(ID, settingId, behavior);
  }
}
