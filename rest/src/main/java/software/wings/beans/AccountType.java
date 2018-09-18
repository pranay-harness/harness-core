package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

/**
 * Account type.
 * @author rktummala on 08/29/18
 */
// Note: This is intentionally not made enum
public interface AccountType {
  String TRIAL = "TRIAL";
  String PAID = "PAID";
  String FREEMIUM = "FREEMIUM";

  static boolean isValid(String type) {
    if (isEmpty(type)) {
      return false;
    }

    switch (type) {
      case TRIAL:
      case PAID:
      case FREEMIUM:
        return true;
      default:
        return false;
    }
  }
}
