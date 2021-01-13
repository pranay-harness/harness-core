package io.harness.cdng.service.beans;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("serviceConfigOutcome")
@JsonTypeName("serviceConfigOutcome")
public class ServiceConfigOutcome implements Outcome {
  ServiceOutcome service;

  @Override
  public String getType() {
    return "serviceConfigOutcome";
  }
}
