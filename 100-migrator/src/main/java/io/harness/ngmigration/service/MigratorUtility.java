/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import io.harness.pms.yaml.ParameterField;

import org.apache.commons.lang3.StringUtils;

public class MigratorUtility {
  public static String generateIdentifier(String name) {
    return name.trim().toLowerCase().replaceAll("[^A-Za-z0-9]", "");
  }

  public static ParameterField<String> getParameterField(String value) {
    if (StringUtils.isBlank(value)) {
      return ParameterField.createValueField("");
    }
    return ParameterField.createValueField(value);
  }
}
