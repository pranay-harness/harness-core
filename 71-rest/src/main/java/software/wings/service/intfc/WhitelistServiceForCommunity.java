package software.wings.service.intfc;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.security.access.Whitelist;
import software.wings.service.impl.WhitelistServiceImpl;

/**
 * Whitelist service implementation to be used for Community Version of Harness
 */
@Slf4j
@Singleton
public class WhitelistServiceForCommunity extends WhitelistServiceImpl implements WhitelistService {
  @Inject private AccountService accountService;

  @Override
  public Whitelist save(Whitelist whitelist) {
    throw new WingsException(ErrorCode.INVALID_ARGUMENT, "IP Whitelisting is not allowed in Community accounts", USER);
  }

  @Override
  public boolean isValidIPAddress(String accountId, String ipAddress) {
    logger.debug("Account is COMMUNITY. No IP Whitelisting - So, all IPs are valid IPs. accountId={}", accountId);
    return true;
  }

  @Override
  public Whitelist update(Whitelist whitelist) {
    throw new WingsException(ErrorCode.INVALID_ARGUMENT, "IP Whitelisting is not allowed in Community accounts", USER);
  }
}
