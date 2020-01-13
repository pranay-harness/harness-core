package io.harness.batch.processing.billing.reader;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.batch.processing.entities.InstanceData;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.batch.item.ItemReader;
import software.wings.WingsBaseTest;

import java.time.Instant;

public class InstanceDataMongoEventReaderTest extends WingsBaseTest {
  @Inject private InstanceDataMongoEventReader instanceDataMongoEventReader;

  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final Instant NOW = Instant.now();
  private final long START_TIME = NOW.getEpochSecond();
  private final long END_TIME = NOW.getEpochSecond();

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testInstanceDataMongoEventReader() {
    ItemReader<InstanceData> reader = instanceDataMongoEventReader.getEventReader(ACCOUNT_ID, START_TIME, END_TIME);
    assertThat(reader).isNotNull();
  }
}
