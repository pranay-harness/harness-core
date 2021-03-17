package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(innerTypeName = "DelegateInsightsDetailsKeys")
@Data
@Builder
public class DelegateInsightsDetails {
  private List<DelegateInsightsBarDetails> insights;
}
