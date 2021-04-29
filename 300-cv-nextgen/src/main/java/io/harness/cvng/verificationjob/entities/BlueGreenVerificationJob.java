package io.harness.cvng.verificationjob.entities;

import static io.harness.cvng.CVConstants.DEFAULT_BLUE_GREEN_JOB_ID;
import static io.harness.cvng.CVConstants.DEFAULT_BLUE_GREEN_JOB_NAME;

import io.harness.cvng.beans.job.BlueGreenVerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.verificationjob.CVVerificationJobConstants;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@FieldNameConstants(innerTypeName = "BlueGreenVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class BlueGreenVerificationJob extends CanaryBlueGreenVerificationJob {
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.BLUE_GREEN;
  }

  @Override
  public void fromDTO(VerificationJobDTO verificationJobDTO) {
    BlueGreenVerificationJobDTO blueGreenVerificationJobDTO = (BlueGreenVerificationJobDTO) verificationJobDTO;
    this.setSensitivity(blueGreenVerificationJobDTO.getSensitivity(),
        VerificationJobDTO.isRuntimeParam(blueGreenVerificationJobDTO.getSensitivity()));
    this.setTrafficSplitPercentage(blueGreenVerificationJobDTO.getTrafficSplitPercentage());
    addCommonFileds(verificationJobDTO);
  }

  @Override
  public VerificationJobDTO getVerificationJobDTO() {
    BlueGreenVerificationJobDTO blueGreenVerificationJobDTO = new BlueGreenVerificationJobDTO();
    blueGreenVerificationJobDTO.setSensitivity(
        getSensitivity() == null ? CVVerificationJobConstants.RUNTIME_STRING : getSensitivity().name());
    blueGreenVerificationJobDTO.setTrafficSplitPercentage(this.getTrafficSplitPercentage());
    populateCommonFields(blueGreenVerificationJobDTO);
    return blueGreenVerificationJobDTO;
  }

  public static class BlueGreenVerificationUpdatableEntity<T extends BlueGreenVerificationJob, D
                                                               extends BlueGreenVerificationJobDTO>
      extends VerificationJobUpdatableEntity<T, D> {
    @Override
    public void setUpdateOperations(UpdateOperations<T> updateOperations, D dto) {
      setCommonOperations(updateOperations, dto);
      updateOperations.set(CanaryVerificationJob.DeploymentVerificationJobKeys.sensitivity, dto.getSensitivity())
          .set(CanaryVerificationJob.DeploymentVerificationJobKeys.trafficSplitPercentage,
              dto.getTrafficSplitPercentage());
    }
  }

  public static BlueGreenVerificationJob createDefaultJob(
      String accountId, String orgIdentifier, String projectIdentifier) {
    BlueGreenVerificationJob verificationJob = BlueGreenVerificationJob.builder()
                                                   .jobName(DEFAULT_BLUE_GREEN_JOB_NAME)
                                                   .identifier(DEFAULT_BLUE_GREEN_JOB_ID)
                                                   .build();
    CanaryBlueGreenVerificationJob.setCanaryBLueGreenDefaultJobParameters(
        verificationJob, accountId, orgIdentifier, projectIdentifier);
    return verificationJob;
  }
}
