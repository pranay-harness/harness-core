package io.harness.batch.processing;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

/**
 * Generic exception to indicate an error during any of the batch processing steps.
 */
public class BatchProcessingException extends WingsException {
  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public BatchProcessingException(String message, Throwable cause) {
    super(message, cause, ErrorCode.BATCH_PROCESSING_ERROR, Level.ERROR, SRE, null);
  }
}
