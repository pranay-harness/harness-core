package io.harness.accesscontrol.common;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.utils.CryptoUtils;

import javax.validation.executable.ValidateOnExecution;
import lombok.experimental.UtilityClass;

@UtilityClass
@ValidateOnExecution
@OwnedBy(HarnessTeam.PL)
public class AccessControlTestUtils {
  public static String getRandomString(int length) {
    return CryptoUtils.secureRandAlphaNumString(length);
  }
}
