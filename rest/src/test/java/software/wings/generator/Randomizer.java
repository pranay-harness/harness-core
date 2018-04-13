package software.wings.generator;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class Randomizer {
  private static final Logger logger = LoggerFactory.getLogger(Randomizer.class);

  @Value
  @AllArgsConstructor
  public static class Seed {
    long value;
  }

  public static EnhancedRandom instance(Seed seed) {
    return EnhancedRandomBuilder.aNewEnhancedRandomBuilder().objectPoolSize(1).seed(seed.getValue()).build();
  }

  public static Seed seed() {
    long value = new Random().nextLong();
    logger.info("The seed value is {}", value);
    return new Seed(value);
  }
}
