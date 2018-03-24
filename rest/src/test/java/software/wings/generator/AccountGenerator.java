package software.wings.generator;

import com.google.inject.Singleton;

import lombok.Setter;
import software.wings.beans.Account;

@Singleton
public class AccountGenerator {
  @Setter Account account;

  // TODO: Very dummy version, implement this
  public Account randomAccount() {
    return account;
  }
}
