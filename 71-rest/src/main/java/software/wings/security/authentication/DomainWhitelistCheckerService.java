package software.wings.security.authentication;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.User;

import java.util.Set;

/**
 * @author Vaibhav Tulsyan
 * 21/May/2019
 */
@Singleton
@NoArgsConstructor
@Slf4j
public class DomainWhitelistCheckerService {
  @Inject private AuthenticationUtils authenticationUtils;

  public boolean isDomainWhitelisted(User user) {
    Account primaryAccount = authenticationUtils.getDefaultAccount(user);
    if (null == primaryAccount) {
      logger.warn("User with UUID {} has null primary account", user.getUuid());
      return false;
    }

    return isDomainWhitelistedInternal(user, primaryAccount);
  }

  private boolean isDomainWhitelistedInternal(User user, Account account) {
    Set<String> filter = account.getWhitelistedDomains();

    if (isEmpty(filter)) {
      return true;
    }

    String email = user.getEmail();

    if (isEmpty(email)) {
      logger.warn("Empty email received from account {}", account.getUuid());
      return false;
    }

    String domain = email.substring(email.indexOf('@') + 1);

    if (!filter.contains(domain)) {
      logger.info("Domain filter was: [{}] while the email was: [{}]", filter, email);
      return false;
    }
    return true;
  }

  public boolean isDomainWhitelisted(User user, Account account) {
    if (null == account) {
      logger.warn("Account received was null for user with UUID {}", user.getUuid());
      return false;
    }
    return isDomainWhitelistedInternal(user, account);
  }

  public void throwDomainWhitelistFilterException() {
    throw new WingsException(ErrorCode.DOMAIN_WHITELIST_FILTER_CHECK_FAILED,
        "Domain name filter failed. Please contact your Account Administrator", USER);
  }
}
