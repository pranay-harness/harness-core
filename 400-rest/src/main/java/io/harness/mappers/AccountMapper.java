package io.harness.mappers;

import io.harness.ng.core.dto.AccountDTO;
import lombok.experimental.UtilityClass;
import software.wings.beans.Account;

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
