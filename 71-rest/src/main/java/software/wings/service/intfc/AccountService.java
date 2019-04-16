package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotBlank;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Account;
import software.wings.beans.FeatureFlag;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.service.impl.analysis.CVEnabledService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
public interface AccountService {
  @ValidationGroups(Create.class) Account save(@Valid Account account);

  @ValidationGroups(Update.class) Account update(@Valid Account account);

  Account getByName(String companyName);

  Account get(String accountId);

  Account getFromCache(String accountId);

  String getAccountStatus(String accountId);

  boolean delete(String accountId);

  boolean getTwoFactorEnforceInfo(String accountId);

  void updateTwoFactorEnforceInfo(String accountId, boolean enabled);

  String suggestAccountName(@NotNull String accountName);

  boolean exists(String accountName);

  Optional<String> getAccountType(String accountId);

  String generateSampleDelegate(String accountId);

  boolean sampleDelegateExists(String accountId);

  /**
   * List.
   *
   * @param request the request
   * @return the list of System Catalogs
   */
  List<Account> list(@NotNull PageRequest<Account> request);

  List<Account> listAccounts(Set<String> excludedAccountIds);

  DelegateConfiguration getDelegateConfiguration(String accountId);

  List<Account> listAllAccounts();

  List<Account> listAllAccountWithDefaultsWithoutLicenseInfo();

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

  boolean setAccountStatus(String accountId, String accountStatus);

  boolean isFeatureFlagEnabled(String featureName, String accountId);

  PageResponse<CVEnabledService> getServices(
      String accountId, User user, PageRequest<String> request, String serviceId);

  List<Service> getServicesBreadCrumb(String accountId, User user);

  /**
   * Start account migration from one cluster to another. Once this step completed, the logged in user can only read,
   * but not update the account configurations.
   */
  boolean startAccountMigration(String accountId);

  /**
   * Once the account migration completed. All existing delegates belonging to this account will be redirected to the
   * new cluster that the account has been migrated into.
   */
  boolean completeAccountMigration(String accountId, String newClusterUrl);

  boolean isAccountLite(String accountId);
}
