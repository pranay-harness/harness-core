package io.harness.cvng.beans.dynatrace;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Arrays;
import java.util.List;
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
  private static final List<String> FIELDS = Arrays.asList("lastSeenTms", "properties", "fromRelationships", "toRelationships");
  private static final String DSL =
      DataCollectionRequest.readDSL("dynatrace-service-details.datacollection", DynatraceServiceDetailsRequest.class);

  private String serviceId = "SERVICE-07334C2062F60949";

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> commonEnvVariables = super.fetchDslEnvVariables();
    String fieldsParam = String.join(",", FIELDS);
    commonEnvVariables.put("fields", fieldsParam);
    commonEnvVariables.put("entitySelector", "entityId(\"".concat("SERVICE-07334C2062F60949").concat("\")"));
    return commonEnvVariables;
  }
}
