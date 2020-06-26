package io.harness.engine.barriers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.barriers.BarrierNode;
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
    BarrierNode barrierNode = BarrierNode.builder().uuid(uuid).identifier("identifier").build();
    BarrierNode savedBarrierNode = barrierService.save(barrierNode);

    assertThat(savedBarrierNode).isNotNull();
    assertThat(savedBarrierNode.getUuid()).isEqualTo(uuid);
    assertThat(savedBarrierNode.getCreatedAt()).isNotNull();
    assertThat(savedBarrierNode.getLastUpdatedAt()).isNotNull();
    assertThat(savedBarrierNode.getVersion()).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldGetSavedBarrierNode() {
    String uuid = generateUuid();
    BarrierNode barrierNode = BarrierNode.builder().uuid(uuid).identifier("identifier").build();
    barrierService.save(barrierNode);

    BarrierNode savedBarrierNode = barrierService.get(barrierNode.getUuid());

    assertThat(savedBarrierNode).isNotNull();
    assertThat(savedBarrierNode.getUuid()).isEqualTo(uuid);
    assertThat(savedBarrierNode.getCreatedAt()).isNotNull();
    assertThat(savedBarrierNode.getLastUpdatedAt()).isNotNull();
    assertThat(savedBarrierNode.getVersion()).isNotNull();
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
    List<BarrierNode> barrierNodes =
        Lists.newArrayList(BarrierNode.builder().uuid(generateUuid()).identifier(identifier).build(),
            BarrierNode.builder().uuid(generateUuid()).identifier(identifier).build(),
            BarrierNode.builder().uuid(generateUuid()).identifier(identifier).build());
    barrierNodeRepository.saveAll(barrierNodes);

    List<BarrierNode> barrierNodeList = barrierService.findByIdentifier(barrierNodes.get(0));

    assertThat(barrierNodeList).isNotNull();
    assertThat(barrierNodeList).containsExactlyInAnyOrderElementsOf(barrierNodes);
  }
}
