package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
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

  void delete(String accountId);

  boolean getTwoFactorEnforceInfo(String accountId);

  void updateTwoFactorEnforceInfo(String accountId, User user, boolean enabled);

  //  Account findOrCreate(String companyName);

  String suggestAccountName(@NotNull String accountName);

  boolean exists(String accountName);

  /**
   * List.
   *
   * @param request the request
   * @return the list of System Catalogs
   */
  List<Account> list(@NotNull PageRequest<Account> request);
}
