package io.harness.governance.pipeline.service;

import static graphql.Assert.assertNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    assertEquals(intialList.size() + 1, list.size());
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

    assertEquals(newlyAdded.getName(), "name-new");
    assertEquals(newlyAdded.getDescription(), "description-new");
    assertNotEmpty(newlyAdded.getRules());
    assertEquals(newlyAdded.getRules().get(0).getWeight(), 10);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testAdd() {
    PipelineGovernanceConfig config = new PipelineGovernanceConfig(
        null, SOME_ACCOUNT_ID, "name", "description", Collections.emptyList(), Collections.emptyList());

    PipelineGovernanceConfig addedConfig = pipelineGovernanceService.add(config);
    assertNotNull(addedConfig.getUuid());
  }
}
