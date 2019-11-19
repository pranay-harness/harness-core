package io.harness.rule;

import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

@Slf4j
public class OwnerWatcherRule extends TestWatcher {
  @Override
  protected void failed(Throwable e, Description description) {
    fileTest(description, "failed");
  }

  @Override
  protected void skipped(AssumptionViolatedException e, Description description) {
    fileTest(description, "skipped");
  }

  private void fileTest(Description description, String type) {
    final Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      return;
    }

    for (String developer : owner.developers()) {
      OwnerRule.fileOwnerAs(developer, type);
    }
  }
}