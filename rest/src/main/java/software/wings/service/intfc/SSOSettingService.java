package software.wings.service.intfc;

import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.sso.SamlSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface SSOSettingService {
  SamlSettings getSamlSettingsByIdpUrl(@NotNull String idpUrl);

  SamlSettings getSamlSettingsByAccountId(@NotNull String accountId);

  @ValidationGroups(Create.class) SamlSettings saveSamlSettings(@Valid SamlSettings settings);

  boolean deleteSamlSettings(@NotNull String accountId);

  SamlSettings getSamlSettingsByOrigin(@NotNull String origin);
}
