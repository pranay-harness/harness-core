/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.exception;

import static io.harness.eraro.ErrorCode.TEMPLATES_LINKED;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import software.wings.beans.EntityType;
import software.wings.beans.template.TemplateType;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class TemplateException extends WingsException {
  private static final String MESSAGE_KEY = "message";
  private static final String TEMPLATE_TYPE_KEY = "templateType";
  private static final String ENTITY_TYPE_KEY = "entityType";

  public TemplateException(String message, ErrorCode errorCode, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, errorCode, Level.ERROR, reportTargets, null);
    param(MESSAGE_KEY, message);
  }

  public static TemplateException templateLinkedException(
      String message, TemplateType templateType, List<EntityType> entityTypes) {
    TemplateException templateException = new TemplateException(message, TEMPLATES_LINKED, null, USER);
    templateException.param(TEMPLATE_TYPE_KEY, templateType.name());
    templateException.param(
        ENTITY_TYPE_KEY, entityTypes.stream().map(Object::toString).collect(Collectors.joining(" or ")));
    return templateException;
  }
}
