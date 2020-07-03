package io.harness.engine.barriers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.barriers.BarrierExecutionInstance;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.barrier.BarrierNodeRepository;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class BarrierServiceImplTest extends OrchestrationTest {
  @Inject private BarrierNodeRepository barrierNodeRepository;
  @Inject BarrierService barrierService;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldSaveBarrierNode() {
    String uuid = generateUuid();
    String planExecutionId = generateUuid();
    BarrierExecutionInstance barrierExecutionInstance =
        BarrierExecutionInstance.builder().uuid(uuid).identifier("identifier").planExecutionId(planExecutionId).build();
    BarrierExecutionInstance savedBarrierExecutionInstance = barrierService.save(barrierExecutionInstance);

    assertThat(savedBarrierExecutionInstance).isNotNull();
    assertThat(savedBarrierExecutionInstance.getUuid()).isEqualTo(uuid);
    assertThat(savedBarrierExecutionInstance.getCreatedAt()).isNotNull();
    assertThat(savedBarrierExecutionInstance.getLastUpdatedAt()).isNotNull();
    assertThat(savedBarrierExecutionInstance.getVersion()).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldGetSavedBarrierNode() {
    String uuid = generateUuid();
    String planExecutionId = generateUuid();
    BarrierExecutionInstance barrierExecutionInstance =
        BarrierExecutionInstance.builder().uuid(uuid).identifier("identifier").planExecutionId(planExecutionId).build();
    barrierService.save(barrierExecutionInstance);

    BarrierExecutionInstance savedBarrierExecutionInstance = barrierService.get(barrierExecutionInstance.getUuid());

    assertThat(savedBarrierExecutionInstance).isNotNull();
    assertThat(savedBarrierExecutionInstance.getUuid()).isEqualTo(uuid);
    assertThat(savedBarrierExecutionInstance.getCreatedAt()).isNotNull();
    assertThat(savedBarrierExecutionInstance.getLastUpdatedAt()).isNotNull();
    assertThat(savedBarrierExecutionInstance.getVersion()).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldThrowInvalidRequestException() {
    String uuid = generateUuid();
    assertThatThrownBy(() -> barrierService.get(uuid))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Barrier not found for id: " + uuid);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldFindAllByIdentifier() {
    String identifier = generateUuid();
    String planExecutionId = generateUuid();
    List<BarrierExecutionInstance> barrierExecutionInstances = Lists.newArrayList(BarrierExecutionInstance.builder()
                                                                                      .uuid(generateUuid())
                                                                                      .identifier(identifier)
                                                                                      .planExecutionId(planExecutionId)
                                                                                      .build(),
        BarrierExecutionInstance.builder()
            .uuid(generateUuid())
            .identifier(identifier)
            .planExecutionId(planExecutionId)
            .build(),
        BarrierExecutionInstance.builder()
            .uuid(generateUuid())
            .identifier(identifier)
            .planExecutionId(planExecutionId)
            .build());
    barrierNodeRepository.saveAll(barrierExecutionInstances);

    List<BarrierExecutionInstance> barrierExecutionInstanceList =
        barrierService.findByIdentifierAndPlanExecutionId(identifier, planExecutionId);

    assertThat(barrierExecutionInstanceList).isNotNull();
    assertThat(barrierExecutionInstanceList).containsExactlyInAnyOrderElementsOf(barrierExecutionInstances);
  }
}
