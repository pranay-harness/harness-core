package io.harness.steps.barriers.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.distribution.barrier.Barrier.State.DOWN;
import static io.harness.distribution.barrier.Barrier.State.STANDING;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationStepsTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.repositories.BarrierNodeRepository;
import io.harness.rule.Owner;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierSetupInfo;
import io.harness.steps.barriers.beans.StageDetail;
import io.harness.testlib.RealMongo;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.internal.util.collections.Sets;
import org.springframework.data.mongodb.core.MongoTemplate;

public class BarrierServiceImplTest extends OrchestrationStepsTestBase {
  @Inject private BarrierNodeRepository barrierNodeRepository;
  @Inject BarrierService barrierService;
  @Inject private MongoTemplate mongoTemplate;

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

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldUpdateState() {
    String uuid = generateUuid();
    String planExecutionId = generateUuid();
    BarrierExecutionInstance barrierExecutionInstance =
        BarrierExecutionInstance.builder().uuid(uuid).identifier("identifier").planExecutionId(planExecutionId).build();
    barrierService.save(barrierExecutionInstance);

    BarrierExecutionInstance savedBarrierExecutionInstance = barrierService.get(barrierExecutionInstance.getUuid());
    assertThat(savedBarrierExecutionInstance).isNotNull();

    barrierService.updateState(savedBarrierExecutionInstance.getUuid(), DOWN);
    BarrierExecutionInstance savedBarrier = barrierService.get(savedBarrierExecutionInstance.getUuid());
    assertThat(savedBarrier).isNotNull();
    assertThat(savedBarrier.getBarrierState()).isEqualTo(DOWN);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldFindByStageIdentifierAndPlanExecutionIdAnsStateIn() {
    String planExecutionId = generateUuid();
    String stageIdentifier = generateUuid();

    List<BarrierExecutionInstance> barrierExecutionInstances = Lists.newArrayList(
        BarrierExecutionInstance.builder()
            .uuid(generateUuid())
            .name(generateUuid())
            .barrierState(STANDING)
            .barrierGroupId(generateUuid())
            .identifier(generateUuid())
            .planExecutionId(planExecutionId)
            .planNodeId(generateUuid())
            .setupInfo(BarrierSetupInfo.builder()
                           .stages(Sets.newSet(StageDetail.builder().identifier(stageIdentifier).build()))
                           .build())
            .build(),
        BarrierExecutionInstance.builder()
            .uuid(generateUuid())
            .name(generateUuid())
            .barrierState(DOWN)
            .barrierGroupId(generateUuid())
            .identifier(generateUuid())
            .planExecutionId(planExecutionId)
            .planNodeId(generateUuid())
            .setupInfo(BarrierSetupInfo.builder()
                           .stages(Sets.newSet(StageDetail.builder().identifier(stageIdentifier).build()))
                           .build())
            .build());
    mongoTemplate.insertAll(barrierExecutionInstances);

    List<BarrierExecutionInstance> barrierNodeExecutions =
        barrierService.findByStageIdentifierAndPlanExecutionIdAnsStateIn(
            stageIdentifier, planExecutionId, Sets.newSet(STANDING));

    assertThat(barrierNodeExecutions).isNotNull();
    assertThat(barrierNodeExecutions.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetBarrierSetupInfoList() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String yamlFile = "barriers.yaml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(yamlFile)), StandardCharsets.UTF_8);

    List<BarrierSetupInfo> barrierSetupInfoList = barrierService.getBarrierSetupInfoList(yaml);

    assertThat(barrierSetupInfoList.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowIOExceptionWhenGetBarrierSetupInfoList() {
    String incorrectYaml = "pipeline: stages: stage";
    assertThatThrownBy(() -> barrierService.getBarrierSetupInfoList(incorrectYaml))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Error while extracting yaml");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidRequestExceptionWhenGetBarrierSetupInfoList() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String yamlFile = "barriers-incorrect.yaml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(yamlFile)), StandardCharsets.UTF_8);

    assertThatThrownBy(() -> barrierService.getBarrierSetupInfoList(yaml))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Barrier Identifier myBarrierId7 was not present in flowControl");
  }
}
