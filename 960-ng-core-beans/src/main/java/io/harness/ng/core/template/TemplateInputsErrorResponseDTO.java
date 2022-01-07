/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.template;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ngexception.beans.templateservice.TemplateInputsErrorDTO;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CDC)
@Schema(
    name = "TemplateInputsErrorResponse", description = "This contains details of the Template Inputs Error Response")
public class TemplateInputsErrorResponseDTO extends ErrorDTO {
  String errorYaml;
  Map<String, TemplateInputsErrorDTO> errorMap;

  public TemplateInputsErrorResponseDTO(Status status, ErrorCode code, String message, String detailedMessage,
      String errorYaml, Map<String, TemplateInputsErrorDTO> errorMap) {
    super(status, code, message, detailedMessage);
    this.errorMap = errorMap;
    this.errorYaml = errorYaml;
  }
}
