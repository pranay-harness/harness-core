package io.harness.cvng.beans.dynatrace;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@JsonTypeName("DYNATRACE_SERVICE_DETAILS")
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "DynatraceServiceDetailsRequestKeys")
public class DynatraceServiceDetailsRequest extends DynatraceRequest {
  private static final String DSL =
      DataCollectionRequest.readDSL("dynatrace-service-details.datacollection", DynatraceServiceDetailsRequest.class);

  private String entityId;

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> commonEnvVariables = super.fetchDslEnvVariables();
    commonEnvVariables.put(DynatraceServiceDetailsRequestKeys.entityId, entityId);
    return commonEnvVariables;
  }
}
