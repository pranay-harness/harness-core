package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public enum HostConnectionType {
  PUBLIC_IP,
  PUBLIC_DNS,
  PRIVATE_IP,
  PRIVATE_DNS
}
