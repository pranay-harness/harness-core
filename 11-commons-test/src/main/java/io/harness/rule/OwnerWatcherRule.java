package io.harness.rule;

import io.harness.rule.OwnerRule.DevInfo;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;

@Slf4j
public class OwnerWatcherRule extends TestWatcher {
  @Override
  protected void failed(Throwable e, Description description) {
    final Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      return;
    }

    for (String email : owner.emails()) {
      final DevInfo devInfo = OwnerRule.getActive().get(email);
      if (devInfo == null) {
        continue;
      }

      String identify = devInfo.getSlack() == null ? email : "@" + devInfo.getSlack();

      try {
        final File file = new File(System.getProperty("java.io.tmpdir") + "/owners/" + identify);
        file.getParentFile().mkdirs();
        if (!file.createNewFile()) {
          logger.debug("The owner {} was already set", identify);
        }
      } catch (Exception ignore) {
        // Ignore the exceptions
      }
    }
  }
}