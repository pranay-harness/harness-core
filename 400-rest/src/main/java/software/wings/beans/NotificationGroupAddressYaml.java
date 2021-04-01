package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Yaml representation of addressesByChannelType in NotificationGroup.
 */
@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class NotificationGroupAddressYaml extends BaseYaml {
  private String channelType;
  private List<String> addresses;

  @Builder
  public NotificationGroupAddressYaml(String channelType, List<String> addresses) {
    this.channelType = channelType;
    this.addresses = addresses;
  }
}
