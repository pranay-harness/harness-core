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
