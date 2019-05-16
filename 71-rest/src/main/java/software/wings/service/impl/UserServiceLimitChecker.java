package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.lib.StaticLimit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.service.intfc.AccountService;

import java.util.List;
import java.util.Set;

@Slf4j
@Singleton
public class UserServiceLimitChecker {
  @Inject private AccountService accountService;
  @Inject private LimitConfigurationService limits;

  public void limitCheck(String accountId, List<User> existingUsersAndInvites, Set<String> emailsToInvite) {
    Account account = accountService.get(accountId);
    if (null == account) {
      logger.error("No account found for accountId={}", accountId);
      return;
    }

    ConfiguredLimit limit = limits.getOrDefault(accountId, ActionType.CREATE_USER);
    if (null == limit) {
      logger.error("No user limit configured. accountId={}", accountId);
      return;
    }

    StaticLimit userLimit = (StaticLimit) limit.getLimit();
    if (existingUsersAndInvites.size() + emailsToInvite.size() > userLimit.getCount()) {
      String err = MessageFormatter.format("You can only add upto {} users.", userLimit.getCount()).getMessage();
      throw new WingsException(ErrorCode.USAGE_LIMITS_EXCEEDED, err, USER);
    }
  }
}
