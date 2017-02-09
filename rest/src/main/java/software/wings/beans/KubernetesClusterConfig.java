package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 2/8/17.
 */
@JsonTypeName("KUBERNETES")
public class KubernetesClusterConfig extends SettingValue {
  /**
   * Instantiates a new setting value.
   *
   */
  public KubernetesClusterConfig() {
    super(SettingVariableTypes.KUBERNETES.name());
  }
}
