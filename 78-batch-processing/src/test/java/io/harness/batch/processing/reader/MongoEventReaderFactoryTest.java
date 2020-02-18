package io.harness.batch.processing.reader;

import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.ROHIT;
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
import software.wings.beans.SettingAttribute;

import java.time.Instant;

public class MongoEventReaderFactoryTest extends WingsBaseTest {
  @Inject private MongoEventReaderFactory mongoEventReaderFactory;

  private final String ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final Instant NOW = Instant.now();
  private final long START_TIME = NOW.getEpochSecond();
  private final long END_TIME = NOW.getEpochSecond();

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testPublishedMessageMongoEventReader() {
    ItemReader<PublishedMessage> reader =
        mongoEventReaderFactory.getEventReader(ACCOUNT_ID, EventTypeConstants.EC2_INSTANCE_INFO, START_TIME, END_TIME);
    assertThat(reader).isNotNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testS3JobConfigReaderEventReader() {
    ItemReader<SettingAttribute> reader = mongoEventReaderFactory.getS3JobConfigReader(ACCOUNT_ID);
    assertThat(reader).isNotNull();
  }
}
