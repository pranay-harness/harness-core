package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Account;
import software.wings.beans.DelegateConfiguration;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.service.impl.analysis.CVEnabledService;
import software.wings.verification.CVConfiguration;

import java.util.Collection;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
public interface AccountService {
  @ValidationGroups(Create.class) Account save(@Valid Account account);

  @ValidationGroups(Update.class) Account update(@Valid Account account);

  Account updateAccountLicense(
      @NotEmpty String accountId, LicenseInfo licenseInfo, String salesContacts, boolean setExpiry);

  Account updateAccountLicense(@NotEmpty String accountId, String accountType, String accountStatus,
      String expiryInDays, String salesContacts, boolean setExpiry);

  String generateLicense(String accountType, String accountStatus, String expiryInDays);

  Account getByName(String companyName);

  long getDefaultTrialExpiryTime();

  Account get(String accountId);

  String getAccountStatus(String accountId);

  Account decryptLicenseInfo(Account account, boolean setExpiryTime);

  void delete(String accountId);

  boolean getTwoFactorEnforceInfo(String accountId);

  void updateTwoFactorEnforceInfo(String accountId, User user, boolean enabled);

  //  Account findOrCreate(String companyName);

  String suggestAccountName(@NotNull String accountName);

  boolean exists(String accountName);

  boolean isAccountDeleted(String accountId);

  boolean isAccountExpired(String accountId);

  void updateAccountLicenseForOnPrem(String encryptedLicenseInfoBase64String);

  /**
   * List.
   *
   * @param request the request
   * @return the list of System Catalogs
   */
  List<Account> list(@NotNull PageRequest<Account> request);

  DelegateConfiguration getDelegateConfiguration(String accountId);

  List<Account> listAllAccounts();

  PageResponse<Account> getAccounts(PageRequest<Account> pageRequest);

  Account getByAccountName(String accountName);

  Account getAccountWithDefaults(String accountId);

  /**
   * List all feaature flags and their statuses
   *
   * @param accountId account id
   * @return list of feature flags
   */
  Collection<FeatureFlag> getFeatureFlags(@NotBlank String accountId);

  boolean isFeatureFlagEnabled(FeatureName featureName, String accountId);

  PageResponse<CVConfiguration> getAllCVServicesForAccount(String accountId, User user, PageRequest<String> request);

  PageResponse<CVEnabledService> getServicesForAccount(
      String accountId, User user, PageRequest<String> request, String serviceId);
}
