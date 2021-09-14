/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service;

import software.wings.beans.Account;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BillingDataPipelineUtils {
  private static final String ACCOUNT_NAME_LABEL_KEY = "account_name";
  private static final String ACCOUNT_TYPE_LABEL_KEY = "account_type";
  private static final String PAID_ACCOUNT_TYPE = "PAID";

  public String modifyStringToComplyRegex(String accountInfo) {
    return accountInfo.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }

  public Map<String, String> getLabelMap(String accountName, String accountType) {
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put(ACCOUNT_NAME_LABEL_KEY, BillingDataPipelineUtils.modifyStringToComplyRegex(accountName));
    labelMap.put(ACCOUNT_TYPE_LABEL_KEY, BillingDataPipelineUtils.modifyStringToComplyRegex(accountType));
    return labelMap;
  }

  public String getAccountType(Account accountInfo) {
    if (accountInfo.getLicenseInfo() != null) {
      return accountInfo.getLicenseInfo().getAccountType();
    }
    return PAID_ACCOUNT_TYPE;
  }
}
