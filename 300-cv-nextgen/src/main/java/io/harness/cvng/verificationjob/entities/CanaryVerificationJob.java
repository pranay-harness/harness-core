/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.verificationjob.entities;

import static io.harness.cvng.CVConstants.DEFAULT_CANARY_JOB_ID;
import static io.harness.cvng.CVConstants.DEFAULT_CANARY_JOB_NAME;

import io.harness.cvng.beans.job.CanaryVerificationJobDTO;
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
@FieldNameConstants(innerTypeName = "CanaryVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
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
    this.setTrafficSplitPercentageV2(canaryVerificationJobDTO.getTrafficSplitPercentage(),
        VerificationJobDTO.isRuntimeParam(canaryVerificationJobDTO.getTrafficSplitPercentage()));
    addCommonFileds(verificationJobDTO);
  }

  @Override
  public VerificationJobDTO getVerificationJobDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
    canaryVerificationJobDTO.setSensitivity(
        getSensitivity() == null ? CVVerificationJobConstants.RUNTIME_STRING : getSensitivity().name());
    canaryVerificationJobDTO.setTrafficSplitPercentage(getTrafficSplitPercentage() == null
            ? CVVerificationJobConstants.RUNTIME_STRING
            : String.valueOf(getTrafficSplitPercentage()));
    populateCommonFields(canaryVerificationJobDTO);
    return canaryVerificationJobDTO;
  }

  public static class CanaryVerificationUpdatableEntity<T extends CanaryVerificationJob, D
                                                            extends CanaryVerificationJobDTO>
      extends VerificationJobUpdatableEntity<T, D> {
    @Override
    public void setUpdateOperations(UpdateOperations<T> updateOperations, D dto) {
      setCommonOperations(updateOperations, dto);
      updateOperations.set(CanaryBlueGreenVerificationJobKeys.sensitivity, dto.getSensitivity())
          .set(CanaryBlueGreenVerificationJobKeys.trafficSplitPercentage, dto.getTrafficSplitPercentage());
    }
  }

  public static CanaryVerificationJob createDefaultJob(
      String accountId, String orgIdentifier, String projectIdentifier) {
    CanaryVerificationJob verificationJob =
        CanaryVerificationJob.builder().jobName(DEFAULT_CANARY_JOB_NAME).identifier(DEFAULT_CANARY_JOB_ID).build();
    CanaryBlueGreenVerificationJob.setCanaryBLueGreenDefaultJobParameters(
        verificationJob, accountId, orgIdentifier, projectIdentifier);
    return verificationJob;
  }
}
