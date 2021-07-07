package software.wings.graphql.datafetcher.delegate;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EnvironmentType;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateScope;

import io.harness.utils.RequestField;
import software.wings.graphql.schema.type.delegate.QLDelegate;
import software.wings.graphql.schema.type.delegate.QLDelegateScope;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@UtilityClass
@Slf4j
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DelegateController {
  public static void populateQLDelegate(Delegate delegate, QLDelegate.QLDelegateBuilder qlDelegateBuilder) {
    qlDelegateBuilder.accountId(delegate.getAccountId())
        .delegateName(delegate.getDelegateName())
        .delegateType(delegate.getDelegateType())
        .description(delegate.getDescription())
        .hostName(delegate.getHostName())
        .pollingModeEnabled(delegate.isPolllingModeEnabled())
        .ip(delegate.getIp())
        .status(delegate.getStatus().toString())
        .delegateProfileId(delegate.getDelegateProfileId())
        .lastHeartBeat(delegate.getLastHeartBeat())
        .build();
  }

  public static void populateDelegateScope(
      QLDelegateScope qlDelegateScope, DelegateScope.DelegateScopeBuilder delegateScopeBuilder) {
    delegateScopeBuilder.accountId(qlDelegateScope.getAccountId())
            .name(qlDelegateScope.getName())
            .applications(qlDelegateScope.getApplications())
            .services(qlDelegateScope.getServices())
            .build();
  }
}
