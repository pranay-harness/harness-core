package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseEntityYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class NotificationGroupYaml extends BaseEntityYaml {
  private List<NotificationGroupAddressYaml> addresses;
  private String defaultNotificationGroupForAccount;

  @Builder
  public NotificationGroupYaml(String type, String harnessApiVersion, List<NotificationGroupAddressYaml> addresses,
      String defaultNotificationGroupForAccount) {
    super(type, harnessApiVersion);
    this.addresses = addresses;
    this.defaultNotificationGroupForAccount = defaultNotificationGroupForAccount;
  }
}
