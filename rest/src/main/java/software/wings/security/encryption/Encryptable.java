package software.wings.security.encryption;

import java.util.List;

/**
 * Created by mike@ on 4/25/17.
 */
public interface Encryptable {
  public String getAccountId();

  public void setAccountId(String accountId);

  public List<String> getEncryptedFieldNames();
}
