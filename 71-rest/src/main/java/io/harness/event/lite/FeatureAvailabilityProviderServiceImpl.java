package io.harness.event.lite;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.service.intfc.AccountService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class FeatureAvailabilityProviderServiceImpl implements FeatureAvailabilityProviderService {
  @Inject private AccountService accountService;

  @Override
  public List<FeatureAvailability> listFeatureAvailability(String accountId) {
    if (accountService.isAccountLite(accountId)) {
      return Arrays.stream(HarnessFeature.values())
          .map(it -> new FeatureAvailability(it, false))
          .collect(Collectors.toList());
    } else {
      return Arrays.stream(HarnessFeature.values())
          .map(it -> new FeatureAvailability(it, true))
          .collect(Collectors.toList());
    }
  }
}
