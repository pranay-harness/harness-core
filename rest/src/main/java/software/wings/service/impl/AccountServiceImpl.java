package software.wings.service.impl;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.ErrorCodes;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AccountService;

import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
@Singleton
@ValidateOnExecution
public class AccountServiceImpl implements AccountService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public Account save(@Valid Account account) {
    wingsPersistence.save(account);
    return account;
  }

  @Override
  public Account get(String accountId) {
    return wingsPersistence.get(Account.class, accountId);
  }

  @Override
  public void delete(String accountId) {
    wingsPersistence.delete(Account.class, accountId);
  }

  @Override
  public Account findOrCreate(String companyName) {
    return wingsPersistence.upsert(wingsPersistence.createQuery(Account.class).field("companyName").equal(companyName),
        wingsPersistence.createUpdateOperations(Account.class)
            .setOnInsert("companyName", companyName)
            .setOnInsert("accountKey", generateAccountKey()));
  }

  @Override
  public Account update(@Valid Account account) {
    wingsPersistence.update(
        account, wingsPersistence.createUpdateOperations(Account.class).set("companyName", account.getCompanyName()));
    return wingsPersistence.get(Account.class, account.getUuid());
  }

  @Override
  public Account getByName(String companyName) {
    return wingsPersistence.executeGetOneQuery(
        wingsPersistence.createQuery(Account.class).field("companyName").equal(companyName));
  }

  private String generateAccountKey() {
    KeyGenerator keyGen = null;
    try {
      keyGen = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      logger.error("Exception while generating account key ", e);
      throw new WingsException(ErrorCodes.DEFAULT_ERROR_CODE);
    }
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();
    byte[] encoded = secretKey.getEncoded();
    return Hex.encodeHexString(encoded);
  }
}
