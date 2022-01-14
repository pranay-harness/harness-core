package io.harness.ng.core.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.AccountDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
public class UserRequestDTO {
  @EqualsAndHashCode.Include String uuid;
  String name;
  String email;
  String passwordHash;
  String accountName;
  String companyName;
  String defaultAccountId;
  String token;
  List<AccountDTO> accounts;
  boolean admin;
  boolean emailVerified;
  boolean twoFactorAuthenticationEnabled;
}
