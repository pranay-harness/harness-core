package io.harness.cvng.verificationjob.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import com.google.common.base.Preconditions;

import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
@FieldNameConstants(innerTypeName = "TestVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TestVerificationJob extends VerificationJob {
  private Sensitivity sensitivity;
  private String baseLineVerificationTaskIdentifier;
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.TEST;
  }

  @Override
  public VerificationJobDTO getVerificationJobDTO() {
    TestVerificationJobDTO testVerificationJobDTO = new TestVerificationJobDTO();
    populateCommonFields(testVerificationJobDTO);
    testVerificationJobDTO.setSensitivity(sensitivity);
    testVerificationJobDTO.setBaselineVerificationTaskIdentifier(baseLineVerificationTaskIdentifier);
    return testVerificationJobDTO;
  }

  @Override
  protected void validateParams() {
    Preconditions.checkNotNull(sensitivity, generateErrorMessageFromParam(TestVerificationJobKeys.sensitivity));
  }

  @Override
  public List<TimeRange> getDataCollectionTimeRanges(Instant startTime) {
    List<TimeRange> ranges = new ArrayList<>();
    for (Instant current = startTime; current.compareTo(startTime.plusMillis(getDuration().toMillis())) < 0;
         current = current.plus(1, ChronoUnit.MINUTES)) {
      ranges.add(TimeRange.builder().startTime(current).endTime(current.plus(1, ChronoUnit.MINUTES)).build());
    }
    return ranges;
  }
}
