package software.wings.graphql.datafetcher.billing;

import static com.google.common.base.Preconditions.checkArgument;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpBillingAccountService;

import java.util.ArrayList;
import java.util.List;

public class BillingAccountDataFetcher implements DataFetcher<List<GcpBillingAccount>> {
  @Inject GcpBillingAccountService gcpBillingAccountService;

  @Override
  public List<GcpBillingAccount> get(DataFetchingEnvironment environment) throws Exception {
    String accountId = getAccountId(environment);
    String uuid = environment.getArgument("uuid");
    String organizationSettingId = environment.getArgument("organizationSettingId");

    List<GcpBillingAccount> gcpBillingAccounts = new ArrayList<>();
    if (isNotEmpty(uuid)) {
      gcpBillingAccounts.add(gcpBillingAccountService.get(uuid));
    } else {
      gcpBillingAccounts.addAll(gcpBillingAccountService.list(accountId, organizationSettingId));
    }
    return gcpBillingAccounts;
  }

  private String getAccountId(DataFetchingEnvironment dataFetchingEnvironment) {
    Object contextObj = dataFetchingEnvironment.getContext();
    checkArgument(contextObj instanceof GraphQLContext, "Not a GraphQL Context");

    GraphQLContext context = (GraphQLContext) contextObj;
    String accountId = context.get("accountId");
    checkArgument(isNotEmpty(accountId), "Cannot extract accountId from environment");

    return accountId;
  }
}
