package io.harness.cvng.core.entities;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.cvng.util.ErrorMessageUtils.generateErrorMessageFromParam;

import io.harness.cvng.beans.TimeRange;
import io.harness.cvng.models.VerificationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "LogCVConfigKeys")
@EqualsAndHashCode(callSuper = true)
public abstract class LogCVConfig extends CVConfig {
  private TimeRange baseline;
  private String query;

  @Override
  public VerificationType getVerificationType() {
    return VerificationType.LOG;
  }

  @Override
  public void validate() {
    super.validate();
    checkNotNull(query, generateErrorMessageFromParam(LogCVConfigKeys.query));
  }
}
