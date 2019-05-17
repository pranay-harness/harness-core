package io.harness.event.model.segment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala
 */
@Value
@Builder
@AllArgsConstructor
public class Traits {
  private String email;
  private String firstName;
  private String lastName;
  private String companyName;
  private String accountId;
  private String accountStatus;
  private String daysLeftInTrial;
  private String userInviteUrl;
  private String oauthProvider;
}
