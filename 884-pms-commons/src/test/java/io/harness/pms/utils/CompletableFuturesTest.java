package io.harness.pms.utils;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class CompletableFuturesTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCompletableFutures() throws ExecutionException, InterruptedException {
    CompletableFutures<Integer> completableFutures = new CompletableFutures<>(Executors.newSingleThreadExecutor());
    for (int i = 0; i < 5; i++) {
      int localIdx = i;
      completableFutures.supplyAsync(() -> localIdx);
    }

    assertThat(completableFutures.allOf().get()).containsExactly(0, 1, 2, 3, 4);
  }
}
