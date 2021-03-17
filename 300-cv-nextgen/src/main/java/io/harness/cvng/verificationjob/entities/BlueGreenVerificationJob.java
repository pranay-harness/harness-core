package io.harness.cvng.verificationjob.entities;

import io.harness.cvng.beans.job.BlueGreenVerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@FieldNameConstants(innerTypeName = "BlueGreenVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
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
    blueGreenVerificationJobDTO.setSensitivity(getSensitivity() == null ? null : getSensitivity().name());
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
}
