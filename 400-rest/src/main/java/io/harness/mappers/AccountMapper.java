package io.harness.mappers;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.dto.GatewayAccountRequestDTO;

import software.wings.beans.Account;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._955_ACCOUNT_MGMT)
public class AccountMapper {
  public static AccountDTO toAccountDTO(Account account) {
    return AccountDTO.builder()
        .identifier(account.getUuid())
        .name(account.getAccountName())
        .companyName(account.getCompanyName())
        .defaultExperience(account.getDefaultExperience())
        .authenticationMechanism(account.getAuthenticationMechanism())
        .isNextGenEnabled(account.isNextGenEnabled())
        .serviceAccountConfig(account.getServiceAccountConfig())
        .build();
  }

  public static Account fromAccountDTO(AccountDTO dto) {
    return Account.Builder.anAccount()
        .withUuid(dto.getIdentifier())
        .withAccountName(dto.getName())
        .withCompanyName(dto.getCompanyName())
        .withDefaultExperience(dto.getDefaultExperience())
        .withNextGenEnabled(dto.isNextGenEnabled())
        .withServiceAccountConfig(dto.getServiceAccountConfig())
        .build();
  }

  public static GatewayAccountRequestDTO toGatewayAccountRequest(Account account) {
    return GatewayAccountRequestDTO.builder()
        .uuid(account.getUuid())
        .accountName(account.getAccountName())
        .companyName(account.getCompanyName())
        .createdFromNG(account.isCreatedFromNG())
        .isNextGenEnabled(account.isNextGenEnabled())
        .defaultExperience(account.getDefaultExperience())
        .build();
  }
}
