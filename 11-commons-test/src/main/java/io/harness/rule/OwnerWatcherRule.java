package io.harness.rule;

import static ch.qos.logback.core.util.OptionHelper.getEnv;

import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

@Slf4j
public class OwnerWatcherRule extends TestWatcher {
  @Override
  protected void failed(Throwable e, Description description) {
    final Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      return;
    }

    if ("portal".equals(getEnv("JOB_BASE_NAME")) || "portal-functional-tests".equals(getEnv("JOB_BASE_NAME"))) {
      OwnerRule.checkForJira(description.getDisplayName(), owner.developers()[0], OwnerRule.PRIORITY_VALUE0);
    }

    fileTest(owner, "failed");
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

    fileTest(owner, type);
  }

  private void fileTest(Owner owner, String type) {
    for (String developer : owner.developers()) {
      OwnerRule.fileOwnerAs(developer, type);
    }
  }
}