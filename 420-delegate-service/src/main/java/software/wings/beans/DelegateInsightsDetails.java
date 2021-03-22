package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(innerTypeName = "DelegateInsightsDetailsKeys")
@Data
@Builder
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public class DelegateInsightsDetails {
  private List<DelegateInsightsBarDetails> insights;
}
