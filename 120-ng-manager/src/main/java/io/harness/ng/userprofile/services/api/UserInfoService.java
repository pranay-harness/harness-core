
package io.harness.ng.userprofile.services.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.UserInfo;

@OwnedBy(HarnessTeam.PL)
public interface UserInfoService {
  UserInfo get();
  UserInfo update(UserInfo userInfo);
}
