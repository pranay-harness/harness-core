package software.wings.service.impl.instana;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.common.VerificationConstants.VERIFICATION_HOST_PLACEHOLDER;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "InfraParamsKeys")
public class InstanaInfraParams {
  private List<String> metrics;
  private String query;

  public void validateFields(Map<String, String> errors) {
    if (isEmpty(metrics)) {
      errors.put("infraParams." + InfraParamsKeys.metrics, "select at least one metric value.");
    }
    if (isEmpty(query)) {
      errors.put("infraParams." + InfraParamsKeys.query, "query is a required field.");
    }
    if (query != null && !query.contains(VERIFICATION_HOST_PLACEHOLDER)) {
      errors.put("infraParams." + InfraParamsKeys.query, "query should contain " + VERIFICATION_HOST_PLACEHOLDER);
    }
  }
}
