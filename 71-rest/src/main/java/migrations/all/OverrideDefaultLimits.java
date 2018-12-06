package migrations.all;

import com.google.inject.Inject;

import io.harness.limits.ActionType;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.RateLimit;
import migrations.Migration;

import java.util.concurrent.TimeUnit;

public class OverrideDefaultLimits implements Migration {
  @Inject private LimitConfigurationService limitConfigurationService;

  // limits decided based on logDNA deployment metrics:
  // https://app.logdna.com/d9a7810a99/graphs/board/23ed1d1712?from=1543382340000&to=1543468740000
  @Override
  public void migrate() {
    // iHerb
    limitConfigurationService.configure(
        "bwBVO7N0RmKltRhTjk101A", ActionType.DEPLOY, new RateLimit(400, 24, TimeUnit.HOURS));
  }
}
