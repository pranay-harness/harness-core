package io.harness.ccm.utils.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.time.Instant;
import lombok.NonNull;

@Singleton
@OwnedBy(CE)
public class GraphQLUtils {
  public static final long DEFAULT_OFFSET = 0L;
  public static final long DEFAULT_LIMIT = 10L;

  public String getAccountIdentifier(@NonNull ResolutionEnvironment env) {
    return getContextValue(env, NGCommonEntityConstants.ACCOUNT_KEY);
  }

  public String getOrgIdentifier(@NonNull ResolutionEnvironment env) {
    return getContextValue(env, NGCommonEntityConstants.ORG_KEY);
  }

  public String getProjectIdentifier(@NonNull ResolutionEnvironment env) {
    return getContextValue(env, NGCommonEntityConstants.PROJECT_KEY);
  }

  private String getContextValue(@NonNull ResolutionEnvironment env, @NonNull String key) {
    checkNotNull(env.dataFetchingEnvironment, "dataFetchingEnvironment is null");
    checkNotNull(env.dataFetchingEnvironment.getContext(), "dataFetchingEnvironment");
    return ((graphql.GraphQLContext) env.dataFetchingEnvironment.getContext()).get(key);
  }

  // helper
  public long currentTime() {
    return Instant.now().toEpochMilli();
  }
}
