package software.wings.graphql.datafetcher.billing;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BillingDataPipelineCacheObject {
  String dataSetId;
  List<String> awsLinkedAccountsToExclude;
  List<String> awsLinkedAccountsToInclude;
}
