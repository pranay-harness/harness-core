package io.harness.mappers;

import io.harness.ng.core.dto.AccountDTO;

import software.wings.beans.Account;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountMapper {
  public static AccountDTO toAccountDTO(Account account) {
    return AccountDTO.builder()
        .identifier(account.getUuid())
        .name(account.getAccountName())
        .companyName(account.getCompanyName())
        .build();
  }
}
