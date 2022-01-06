/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class NGTemplateConstants {
  public static final String TEMPLATE = "template";
  public static final String TEMPLATE_REF = "templateRef";
  public static final String TEMPLATE_VERSION_LABEL = "versionLabel";
  public static final String TEMPLATE_INPUTS = "templateInputs";
  public static final String DUMMY_NODE = "dummy";
  public static final String SPEC = "spec";
  public static final String IDENTIFIER = "identifier";
  public static final String NAME = "name";
}
