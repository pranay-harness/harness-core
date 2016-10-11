package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Account;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
public interface AccountService {
  @ValidationGroups(Create.class) Account save(@Valid Account account);

  @ValidationGroups(Update.class) Account update(@Valid Account account);

  Account getByName(String companyName);

  Account get(String accountId);

  void delete(String accountId);
}
