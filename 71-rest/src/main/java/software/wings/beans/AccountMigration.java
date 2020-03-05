package software.wings.beans;

import java.util.Optional;

public enum AccountMigration {
  ESSENTIALS_TO_PAID(AccountType.ESSENTIALS, AccountType.PAID, false),
  TRIAL_TO_PAID(AccountType.TRIAL, AccountType.PAID, false),
  PAID_TO_TRIAL(AccountType.PAID, AccountType.TRIAL, false),
  PAID_TO_ESSENTIALS(AccountType.PAID, AccountType.ESSENTIALS, false),
  TRIAL_TO_ESSENTIALS(AccountType.TRIAL, AccountType.ESSENTIALS, false),
  TRIAL_TO_COMMUNITY(AccountType.TRIAL, AccountType.COMMUNITY, true),
  COMMUNITY_TO_PAID(AccountType.COMMUNITY, AccountType.PAID, false),
  COMMUNITY_TO_ESSENTIALS(AccountType.COMMUNITY, AccountType.ESSENTIALS, false),
  COMMUNITY_TO_TRIAL(AccountType.COMMUNITY, AccountType.TRIAL, false);

  private final String from;
  private final String to;
  private boolean selfService;

  AccountMigration(String from, String to, boolean selfService) {
    this.from = from;
    this.to = to;
    this.selfService = selfService;
  }

  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }

  public boolean isSelfService() {
    return selfService;
  }

  public static Optional<AccountMigration> from(String from, String to) {
    AccountMigration result = null;
    for (AccountMigration transition : AccountMigration.values()) {
      if (transition.from.equals(from) && transition.to.equals(to)) {
        result = transition;
      }
    }

    return Optional.ofNullable(result);
  }
}
