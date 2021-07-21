package io.harness.account.services;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.signup.dto.SignupDTO;

@OwnedBy(GTM)
public interface AccountService {
  AccountDTO createAccount(SignupDTO dto) throws WingsException;
  Boolean updateDefaultExperienceIfApplicable(String accountId, DefaultExperience defaultExperience);
  String getBaseUrl(String accountId);
  AccountDTO getAccount(String accountId);
}
