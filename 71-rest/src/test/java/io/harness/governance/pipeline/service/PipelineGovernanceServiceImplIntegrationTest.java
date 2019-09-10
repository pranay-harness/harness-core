package io.harness.governance.pipeline.service;

import static graphql.Assert.assertNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.governance.pipeline.model.MatchType;
import io.harness.governance.pipeline.model.PipelineGovernanceConfig;
import io.harness.governance.pipeline.model.PipelineGovernanceRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtils;

import java.util.Collections;
import java.util.List;

public class PipelineGovernanceServiceImplIntegrationTest extends BaseIntegrationTest {
  @Inject private PipelineGovernanceService pipelineGovernanceService;
  @Inject private WingsPersistence persistence;

  private final String SOME_ACCOUNT_ID =
      "some-account-id-" + PipelineGovernanceServiceImplIntegrationTest.class.getSimpleName();

  private boolean indexesEnsured;

  @Before
  public void init() throws Exception {
    if (!indexesEnsured && !IntegrationTestUtils.isManagerRunning(client)) {
      persistence.getDatastore(PipelineGovernanceConfig.class).ensureIndexes(PipelineGovernanceConfig.class);
      indexesEnsured = true;
    }

    persistence.delete(persistence.createQuery(PipelineGovernanceConfig.class));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testList() {
    List<PipelineGovernanceConfig> intialList = pipelineGovernanceService.list(SOME_ACCOUNT_ID);

    PipelineGovernanceConfig config = new PipelineGovernanceConfig(
        null, SOME_ACCOUNT_ID, "name", "description", Collections.emptyList(), Collections.emptyList());

    pipelineGovernanceService.add(config);

    List<PipelineGovernanceConfig> list = pipelineGovernanceService.list(SOME_ACCOUNT_ID);
    assertThat(list.size()).isEqualTo(intialList.size() + 1);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testUpdate() {
    PipelineGovernanceConfig config = new PipelineGovernanceConfig(
        null, SOME_ACCOUNT_ID, "name", "description", Collections.emptyList(), Collections.emptyList());

    PipelineGovernanceConfig addedConfig = pipelineGovernanceService.add(config);
    PipelineGovernanceConfig newlyAdded = pipelineGovernanceService.update(addedConfig.getUuid(),
        new PipelineGovernanceConfig(null, SOME_ACCOUNT_ID, "name-new", "description-new",
            Collections.singletonList(new PipelineGovernanceRule(Collections.emptyList(), MatchType.ALL, 10, "", true)),
            Collections.emptyList()));

    assertThat("name-new").isEqualTo(newlyAdded.getName());
    assertThat("description-new").isEqualTo(newlyAdded.getDescription());
    assertNotEmpty(newlyAdded.getRules());
    assertThat(10).isEqualTo(newlyAdded.getRules().get(0).getWeight());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testAdd() {
    PipelineGovernanceConfig config = new PipelineGovernanceConfig(
        null, SOME_ACCOUNT_ID, "name", "description", Collections.emptyList(), Collections.emptyList());

    PipelineGovernanceConfig addedConfig = pipelineGovernanceService.add(config);
    assertThat(addedConfig.getUuid()).isNotNull();
  }
}
