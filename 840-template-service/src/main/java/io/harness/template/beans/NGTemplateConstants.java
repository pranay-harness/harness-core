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
