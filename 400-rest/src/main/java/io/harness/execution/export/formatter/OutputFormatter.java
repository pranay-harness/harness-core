/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.formatter;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.export.metadata.ExecutionMetadata;
import io.harness.execution.export.request.ExportExecutionsRequest.OutputFormat;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface OutputFormatter {
  byte[] getExecutionMetadataOutputBytes(ExecutionMetadata executionMetadata);
  String getExtension();

  static OutputFormatter fromOutputFormat(@NotNull OutputFormat format) {
    if (format == OutputFormat.JSON) {
      return new JsonFormatter();
    }

    throw new InvalidRequestException(
        format("Unsupported output format [%s] for export executions request", format.name()));
  }
}
