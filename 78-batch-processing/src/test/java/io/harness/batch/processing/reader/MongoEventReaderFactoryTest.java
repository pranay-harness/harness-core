package io.harness.batch.processing.reader;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.batch.item.ItemReader;
import software.wings.WingsBaseTest;

import java.time.Instant;

public class MongoEventReaderFactoryTest extends WingsBaseTest {
  @Inject private MongoEventReaderFactory mongoEventReaderFactory;

  private final Instant NOW = Instant.now();
  private final long START_TIME = NOW.getEpochSecond();
  private final long END_TIME = NOW.getEpochSecond();

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testPublishedMessageMongoEventReader() {
    ItemReader<PublishedMessage> reader =
        mongoEventReaderFactory.getEventReader(EventTypeConstants.EC2_INSTANCE_INFO, START_TIME, END_TIME);
    assertThat(reader).isNotNull();
  }
}
