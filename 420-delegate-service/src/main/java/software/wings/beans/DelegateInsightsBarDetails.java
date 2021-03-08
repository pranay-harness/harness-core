package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.tuple.Pair;

@FieldNameConstants(innerTypeName = "DelegateInsightsBarDetailsKeys")
@Data
@Builder
public class DelegateInsightsBarDetails {
  private long timeStamp;
  private List<Pair<DelegateInsightsType, Integer>> counts;
}
