package software.wings.graphql.datafetcher.billing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class BillingDataPipelineCacheObject {
  String dataSetId;
  List<String> awsLinkedAccountsToExclude;
  List<String> awsLinkedAccountsToInclude;
}
