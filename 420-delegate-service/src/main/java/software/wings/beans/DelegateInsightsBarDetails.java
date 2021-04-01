package software.wings.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.DEL)
@FieldNameConstants(innerTypeName = "DelegateInsightsBarDetailsKeys")
@Data
@Builder
public class DelegateInsightsBarDetails {
  private long timeStamp;
  @Default private List<Pair<DelegateInsightsType, Long>> counts = new ArrayList<>();
}
