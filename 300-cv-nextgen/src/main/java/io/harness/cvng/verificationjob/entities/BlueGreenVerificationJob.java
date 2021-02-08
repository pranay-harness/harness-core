package io.harness.cvng.verificationjob.entities;

import io.harness.cvng.beans.job.BlueGreenVerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

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
}
