package software.wings.licensing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.License;

import java.util.List;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
@OwnedBy(HarnessTeam.GTM)
@TargetModule(HarnessModule._945_ACCOUNT_MGMT)
public interface LicenseProvider {
  List<License> getActiveLicenses();
  License get(String licenseId);
}
