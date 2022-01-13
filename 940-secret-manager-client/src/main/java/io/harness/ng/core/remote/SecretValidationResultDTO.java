/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "SecretValidationResult", description = "This has validation details for the Secret defined in Harness.")
public class SecretValidationResultDTO {
  @Schema(
      description =
          "This has the validation status for a secret. It is Success, if validation is successful, else the status is Failed.")
  private boolean success;
  @Schema(description = "This is the error message when validation for secret fails.") private String message;
}
