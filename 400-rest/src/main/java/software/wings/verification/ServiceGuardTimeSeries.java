package software.wings.verification;

import java.util.Set;
import java.util.SortedSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceGuardTimeSeries {
  SortedSet<TransactionTimeSeries> timeSeriesSet;
  Set<String> transactionsInAnalysis;
  int totalRecords;
}
