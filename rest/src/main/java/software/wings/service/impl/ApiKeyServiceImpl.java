package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;
import static org.mindrot.jbcrypt.BCrypt.checkpw;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mindrot.jbcrypt.BCrypt;
import software.wings.beans.Account;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.Base;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.UnauthorizedException;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ApiKeyService;
import software.wings.utils.CryptoUtil;
import software.wings.utils.Validator;

import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class ApiKeyServiceImpl implements ApiKeyService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;

  private SimpleEncryption getSimpleEncryption(String accountId) {
    Account account = accountService.get(accountId);
    Validator.notNullCheck("Account", account);
    return new SimpleEncryption(account.getAccountKey().toCharArray());
  }

  @Override
  public String generate(String accountId) {
    int KEY_LEN = 80;
    String apiKey = CryptoUtil.secureRandAlphaNumString(KEY_LEN);
    wingsPersistence.save(ApiKeyEntry.builder()
                              .appId(Base.GLOBAL_APP_ID)
                              .encryptedKey(getSimpleEncryption(accountId).encryptChars(apiKey.toCharArray()))
                              .hashOfKey(hashpw(apiKey, BCrypt.gensalt()))
                              .accountId(accountId)
                              .build());
    return apiKey;
  }

  @Override
  public PageResponse<ApiKeyEntry> list(PageRequest<ApiKeyEntry> pageRequest) {
    return wingsPersistence.query(ApiKeyEntry.class, pageRequest);
  }

  @Override
  public String get(String uuid, String accountId) {
    ApiKeyEntry entry =
        wingsPersistence.createQuery(ApiKeyEntry.class).filter(ACCOUNT_ID_KEY, accountId).filter(ID_KEY, uuid).get();
    Validator.notNullCheck("apiKeyEntry", entry);
    return new String(getSimpleEncryption(accountId).decryptChars(entry.getEncryptedKey()));
  }

  @Override
  public void delete(String uuid) {
    wingsPersistence.delete(ApiKeyEntry.class, uuid);
  }

  @Override
  public void validate(String key, String accountId) {
    PageRequest<ApiKeyEntry> pageRequest = aPageRequest().addFilter("accountId", EQ, accountId).build();
    if (!wingsPersistence.query(ApiKeyEntry.class, pageRequest)
             .getResponse()
             .stream()
             .map(apiKeyEntry -> checkpw(key, apiKeyEntry.getHashOfKey()))
             .collect(Collectors.toSet())
             .contains(true)) {
      throw new UnauthorizedException("Invalid Api Key", USER);
    }
  }
}