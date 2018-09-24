package software.wings.licensing;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import software.wings.beans.License;
import software.wings.dl.WingsPersistence;

import java.util.List;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
public class DatabaseLicenseProviderImpl implements LicenseProvider {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public List<License> getActiveLicenses() {
    return wingsPersistence.createQuery(License.class, excludeAuthority).asList();
  }

  @Override
  public License get(String licenseId) {
    return wingsPersistence.get(License.class, licenseId);
  }
}
