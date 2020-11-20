package io.harness.state.io;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import io.harness.OrchestrationBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.refObjects.RefObjectUtil;
import io.harness.rule.Owner;
import io.harness.utils.DummyOutcome;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class StepInputPackageTest extends OrchestrationBeansTestBase {
  StepInputPackage inputPackage =
      StepInputPackage.builder()
          .input(ResolvedRefInput.builder()
                     .refObject(RefObjectUtil.getSweepingOutputRefObject("refName1", "refKey", generateUuid()))
                     .transput(new DummyOutcome("name1"))
                     .build())
          .input(ResolvedRefInput.builder()
                     .refObject(RefObjectUtil.getSweepingOutputRefObject("refName2", "refKey", generateUuid()))
                     .transput(new DummyOutcome("name2"))
                     .build())
          .input(ResolvedRefInput.builder()
                     .refObject(RefObjectUtil.getSweepingOutputRefObject("refName1", "refKeyBlah", generateUuid()))
                     .transput(new DummyOutcome("name3"))
                     .build())
          .build();
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFindByRefKey() {
    assumeThat(false).isTrue();
    List<StepTransput> transputList = inputPackage.findByRefKey("refKey");
    assertThat(transputList).isNotEmpty();
    assertThat(transputList).hasSize(2);
    assertThat(transputList.stream().map(transput -> ((DummyOutcome) transput).getName()))
        .containsExactly("name1", "name2");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldReturnEmptyIfNoKey() {
    List<StepTransput> transputList = inputPackage.findByRefKey("refKeyDummy");
    assertThat(transputList).isEmpty();
  }
}
