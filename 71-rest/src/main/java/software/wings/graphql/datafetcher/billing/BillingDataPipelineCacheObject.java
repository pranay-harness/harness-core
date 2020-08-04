package software.wings.graphql.datafetcher.billing;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BillingDataPipelineCacheObject {
  String dataSetId;
  List<String> awsLinkedAccountsToExclude;
}
