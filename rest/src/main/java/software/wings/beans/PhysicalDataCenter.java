package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("PHYSICAL_DATA_CENTER")
public class PhysicalDataCenter extends SettingValue {
  /**
   * Instantiates a new setting value.
   *
   */
  public PhysicalDataCenter() {
    super(SettingVariableTypes.PHYSICAL_DATA_CENTER.name());
  }
}
