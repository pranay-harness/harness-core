/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.setup.util;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.math.BigDecimal;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(CE)
public class InfraSetUpUtils {
  private static final String EXTERNAL_ID_TEMPLATE = "harness:%s:%s";
  private static final String LINKED_ACCOUNT_ARN_TEMPLATE = "arn:aws:iam::%s:role/%s";

  public static String getAwsExternalId(String harnessAccountId, String customerId) {
    return String.format(EXTERNAL_ID_TEMPLATE, harnessAccountId, customerId);
  }

  public static String getLinkedAccountArn(String infraAccountId, String roleName) {
    return String.format(LINKED_ACCOUNT_ARN_TEMPLATE, infraAccountId, roleName);
  }

  public static String getCEAwsAccountId(String awsAccountId) {
    try {
      awsAccountId = String.valueOf(new BigDecimal(awsAccountId).longValue());
    } catch (Exception ex) {
      log.error("Exception while getting accountId {}", ex);
    }
    return awsAccountId;
  }
}
