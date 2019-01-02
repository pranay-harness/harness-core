package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import org.apache.commons.lang3.StringUtils;

/**
 * Account status.
 * @author rktummala on 08/30/18
 */
// Note: This is intentionally not made enum
public interface AccountStatus {
  String ACTIVE = "ACTIVE";
  String EXPIRED = "EXPIRED";
  String DELETED = "DELETED";
  String MIGRATING = "MIGRATING";
  String MIGRATED = "MIGRATED";

  static boolean isValid(String status) {
    if (isEmpty(status)) {
      return false;
    }

    switch (StringUtils.upperCase(status)) {
      case ACTIVE:
      case EXPIRED:
      case DELETED:
      case MIGRATING:
      case MIGRATED:
        return true;
      default:
        return false;
    }
  }
}
