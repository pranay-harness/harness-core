package io.harness.cvng.verificationjob.beans;

import io.harness.cvng.beans.job.VerificationJobType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class AdditionalInfo {
  public abstract VerificationJobType getType();
}
