package io.harness.cvng.verificationjob.entities;

import io.harness.cvng.beans.job.CanaryVerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@FieldNameConstants(innerTypeName = "CanaryVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CanaryVerificationJob extends CanaryBlueGreenVerificationJob {
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.CANARY;
  }

  @Override
  public void fromDTO(VerificationJobDTO verificationJobDTO) {
    CanaryVerificationJobDTO canaryVerificationJobDTO = (CanaryVerificationJobDTO) verificationJobDTO;
    this.setSensitivity(canaryVerificationJobDTO.getSensitivity(),
        VerificationJobDTO.isRuntimeParam(canaryVerificationJobDTO.getSensitivity()));
    this.setTrafficSplitPercentage(canaryVerificationJobDTO.getTrafficSplitPercentage());
    addCommonFileds(verificationJobDTO);
  }

  @Override
  public VerificationJobDTO getVerificationJobDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
    canaryVerificationJobDTO.setSensitivity(getSensitivity() == null ? null : getSensitivity().name());
    canaryVerificationJobDTO.setTrafficSplitPercentage(this.getTrafficSplitPercentage());
    populateCommonFields(canaryVerificationJobDTO);
    return canaryVerificationJobDTO;
  }

  public static class CanaryVerificationUpdatableEntity<T extends CanaryVerificationJob, D
                                                            extends CanaryVerificationJobDTO>
      extends VerificationJobUpdatableEntity<T, D> {
    @Override
    public void setUpdateOperations(UpdateOperations<T> updateOperations, D dto) {
      setCommonOperations(updateOperations, dto);
      updateOperations.set(CanaryVerificationJob.DeploymentVerificationJobKeys.sensitivity, dto.getSensitivity())
          .set(CanaryVerificationJob.DeploymentVerificationJobKeys.trafficSplitPercentage,
              dto.getTrafficSplitPercentage());
    }
  }
}
