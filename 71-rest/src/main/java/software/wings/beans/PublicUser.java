package software.wings.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicUser {
  User user;
  boolean inviteAccepted;
}