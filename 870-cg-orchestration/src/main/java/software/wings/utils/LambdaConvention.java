/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.regex.Pattern;

@OwnedBy(CDP)
public class LambdaConvention {
  private static final String DASH = "-";
  private static Pattern wildCharPattern = Pattern.compile("[+*/\\\\ &$|\"']");

  public static String normalizeFunctionName(String functionName) {
    return wildCharPattern.matcher(functionName).replaceAll(DASH);
  }
}
