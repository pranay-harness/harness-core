package io.harness.feature.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.feature.example.RateLimitExampleImpl;
import io.harness.feature.example.StaticLimitExampleImpl;
import io.harness.feature.interfaces.RateLimitInterface;
import io.harness.feature.interfaces.StaticLimitInterface;
import io.harness.rule.Owner;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(GTM)
public class FeaturesManagementJobImplTest extends CategoryTest {
  @InjectMocks FeaturesManagementJobImpl featuresManagementJob;
  @Mock FeatureServiceImpl featureService;
  @Mock Injector injector;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testFeaturesManagementJobInitialization() throws ClassNotFoundException {
    Class<StaticLimitInterface> staticImplClass =
        (Class<StaticLimitInterface>) Class.forName("io.harness.feature.example.StaticLimitExampleImpl");
    Class<RateLimitInterface> rateImplClass =
        (Class<RateLimitInterface>) Class.forName("io.harness.feature.example.RateLimitExampleImpl");
    when(injector.getInstance(staticImplClass)).thenReturn(new StaticLimitExampleImpl());
    when(injector.getInstance(rateImplClass)).thenReturn(new RateLimitExampleImpl());
    featuresManagementJob.run(injector);

    verify(featureService, times(3)).registerFeature(anyString(), any());
  }
}
