package io.harness.ng.userprofile.services.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.TwoFactorAuthMechanismInfo;
import io.harness.ng.core.user.TwoFactorAuthSettingsInfo;
import io.harness.ng.core.user.UserInfo;

@OwnedBy(HarnessTeam.PL)
public interface UserInfoService {
  UserInfo get();
  UserInfo update(UserInfo userInfo);
  TwoFactorAuthSettingsInfo getTwoFactorAuthSettingsInfo(TwoFactorAuthMechanismInfo twoFactorAuthMechanismInfo);
  UserInfo updateTwoFactorAuthInfo(TwoFactorAuthSettingsInfo authSettingsInfo);
  UserInfo disableTFA();
}
