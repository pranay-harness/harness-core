package software.wings.licensing;

import software.wings.beans.License;

import java.util.List;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
public interface LicenseProvider {
  List<License> getActiveLicenses();
  License get(String licenseId);
}
