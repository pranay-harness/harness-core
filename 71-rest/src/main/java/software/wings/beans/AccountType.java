package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import org.apache.commons.lang3.StringUtils;

/**
 * Account type.
 * @author rktummala on 08/29/18
 */
// Note: This is intentionally not made enum
public interface AccountType {
  String TRIAL = "TRIAL";
  String PAID = "PAID";
  String FREE = "FREE";

  static boolean isValid(String type) {
    if (isEmpty(type)) {
      return false;
    }

    switch (StringUtils.upperCase(type)) {
      case TRIAL:
      case PAID:
      case FREE:
        return true;
      default:
        return false;
    }
  }
}
