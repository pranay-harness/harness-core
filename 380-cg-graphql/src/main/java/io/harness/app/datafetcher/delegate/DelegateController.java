package io.harness.app.datafetcher.delegate;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateScope;

import io.harness.app.schema.type.delegate.QLDelegate;
import io.harness.app.schema.type.delegate.QLDelegateScope;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
            .build();
  }
}
