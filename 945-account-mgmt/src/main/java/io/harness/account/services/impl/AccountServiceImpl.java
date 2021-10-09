package io.harness.account.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.account.AccountClient;
import io.harness.account.services.AccountService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.remote.client.RestClientUtils;
import io.harness.signup.dto.SignupDTO;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@OwnedBy(GTM)
public class AccountServiceImpl implements AccountService {
  private final AccountClient accountClient;

  @Override
  public AccountDTO createAccount(SignupDTO dto) {
    String username = dto.getEmail().split("@")[0];

    AccountDTO accountDTO =
        AccountDTO.builder().name(username).companyName(username).defaultExperience(DefaultExperience.NG).build();

    return RestClientUtils.getResponse(accountClient.create(accountDTO));
  }

  @Override
  public Boolean updateDefaultExperienceIfApplicable(String accountId, DefaultExperience defaultExperience) {
    return RestClientUtils.getResponse(accountClient.updateDefaultExperienceIfApplicable(accountId, defaultExperience));
  }

  @Override
  public String getBaseUrl(String accountId) {
    return RestClientUtils.getResponse(accountClient.getBaseUrl(accountId));
  }

  @Override
  public AccountDTO getAccount(String accountId) {
    return RestClientUtils.getResponse(accountClient.getAccountDTO(accountId));
  }
}
