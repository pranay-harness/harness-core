package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 2/8/17.
 */
@JsonTypeName("ECS")
public class EcsClusterConfig extends SettingValue {
  public EcsClusterConfig() {
    super(SettingVariableTypes.ECS.name());
  }
}
