package software.wings.service.impl;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureFlagKeys;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Collections;

public class FeatureFlagServiceImplTest extends WingsBaseTest {
  @Mock WingsPersistence wingsPersistence;

  @Inject @InjectMocks FeatureFlagService featureFlagService;

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldEnableWhenFeatureFlagNotAlreadyPresent() {
    Query query = mock(Query.class);
    doReturn(query).when(wingsPersistence).createQuery(FeatureFlag.class);
    doReturn(query).when(query).filter(FeatureFlagKeys.name, FeatureName.INFRA_MAPPING_REFACTOR.name());
    doReturn(null).when(query).get();
    FeatureFlag featureFlag = FeatureFlag.builder()
                                  .name(FeatureName.INFRA_MAPPING_REFACTOR.name())
                                  .accountIds(Collections.singleton(ACCOUNT_ID))
                                  .build();
    doReturn(null).when(wingsPersistence).save(featureFlag);

    featureFlagService.enableAccount(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID);

    verify(wingsPersistence).save(featureFlag);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldEnableWhenNullAccountIds() {
    Query query = mock(Query.class);
    doReturn(query).when(wingsPersistence).createQuery(FeatureFlag.class);
    doReturn(query).when(query).filter(FeatureFlagKeys.name, FeatureName.INFRA_MAPPING_REFACTOR.name());
    doReturn(FeatureFlag.builder().name(FeatureName.INFRA_MAPPING_REFACTOR.name()).build()).when(query).get();
    FeatureFlag featureFlag = FeatureFlag.builder()
                                  .name(FeatureName.INFRA_MAPPING_REFACTOR.name())
                                  .accountIds(Collections.singleton(ACCOUNT_ID))
                                  .build();
    doReturn(null).when(wingsPersistence).save(featureFlag);

    featureFlagService.enableAccount(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID);

    verify(wingsPersistence).save(featureFlag);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldEnableWhenSomeAccountsPresent() {
    Query query = mock(Query.class);
    doReturn(query).when(wingsPersistence).createQuery(FeatureFlag.class);
    doReturn(query).when(query).filter(FeatureFlagKeys.name, FeatureName.INFRA_MAPPING_REFACTOR.name());
    FeatureFlag input = FeatureFlag.builder()
                            .name(FeatureName.INFRA_MAPPING_REFACTOR.name())
                            .accountIds(Sets.newHashSet("ID_1"))
                            .build();
    doReturn(input).when(query).get();
    FeatureFlag featureFlag = FeatureFlag.builder()
                                  .name(FeatureName.INFRA_MAPPING_REFACTOR.name())
                                  .accountIds(Sets.newHashSet("ID_2, ID_1"))
                                  .build();
    doReturn(null).when(wingsPersistence).save(featureFlag);

    featureFlagService.enableAccount(FeatureName.INFRA_MAPPING_REFACTOR, "ID_2");

    ArgumentCaptor<FeatureFlag> pageRequestArgumentCaptor = ArgumentCaptor.forClass(FeatureFlag.class);
    verify(wingsPersistence).save(pageRequestArgumentCaptor.capture());
    assertThat(pageRequestArgumentCaptor.getValue().getAccountIds()).hasSize(2);
  }
}