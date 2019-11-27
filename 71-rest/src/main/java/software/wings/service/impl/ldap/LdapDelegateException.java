package software.wings.service.impl.ldap;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

/**
 * @author deepakPatankar on 11/27/19
 */
public class LdapDelegateException extends WingsException {
  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public LdapDelegateException(String message, Throwable throwable) {
    super(message, throwable, GENERAL_ERROR, Level.ERROR, null, null);
  }
}
