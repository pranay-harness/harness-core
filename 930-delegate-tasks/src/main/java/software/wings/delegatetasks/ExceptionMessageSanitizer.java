/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks;

import static io.harness.expression.SecretString.SECRET_MASK;

import static org.apache.commons.lang3.StringUtils.replaceEach;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.CDP)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class ExceptionMessageSanitizer {
  public static void sanitizeException(Throwable ex, List<String> secrets)
      throws NoSuchFieldException, IllegalAccessException {
    Throwable exception = ex;
    while (exception != null) {
      sanitizeExceptionInternal(exception, secrets);
      exception = exception.getCause();
    }
  }

  protected static void sanitizeExceptionInternal(Throwable exception, List<String> secrets)
      throws NoSuchFieldException, IllegalAccessException {
    String message = exception.getMessage();
    String updatedMessage = sanitizeMessage(message, secrets);
    updateExceptionMessage(exception, updatedMessage);
  }

  protected static String sanitizeMessage(String message, List<String> secrets) {
    ArrayList<String> secretMasks = new ArrayList<>();
    ArrayList<String> secretValues = new ArrayList<>();
    for (String secret : secrets) {
      secretMasks.add(SECRET_MASK);
      secretValues.add(secret);
    }
    return replaceEach(message, secretValues.toArray(new String[] {}), secretMasks.toArray(new String[] {}));
  }

  protected static void updateExceptionMessage(Throwable exception, String message)
      throws NoSuchFieldException, IllegalAccessException {
    Field detailMessageField = Throwable.class.getDeclaredField("detailMessage");
    try {
      if (!detailMessageField.isAccessible()) {
        detailMessageField.setAccessible(true);
      }
      detailMessageField.set(exception, message);
    } finally {
      detailMessageField.setAccessible(false);
    }
  }
}
