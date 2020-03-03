package io.harness.secret;

import com.google.inject.Inject;

import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.stream.BoundedInputStream;
import software.wings.beans.Account;
import software.wings.service.intfc.security.SecretManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

public class EncryptedFileHelper {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  private String accountId;
  private String secretName = "secretName";
  private String secret = "secret";
  @Inject SecretManager secretManager;

  public String createEncryptedFile(String name) {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
    InputStream is = new ByteArrayInputStream(Charset.forName("UTF-16").encode("test").array());
    BoundedInputStream boundedInputStream = new BoundedInputStream(is);
    return secretManager.saveFile(accountId, null, name, 120, null, boundedInputStream);
  }
}
