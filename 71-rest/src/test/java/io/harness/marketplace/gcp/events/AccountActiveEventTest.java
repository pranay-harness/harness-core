package io.harness.marketplace.gcp.events;

import static io.harness.rule.OwnerRule.JATIN;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.app.WingsApplication;

import java.io.IOException;

public class AccountActiveEventTest extends WingsBaseTest {
  @Test
  @Owner(developers = JATIN)
  @Category(UnitTests.class)
  public void testSerialization() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    WingsApplication.configureObjectMapper(mapper);

    AccountActiveEvent event = mapper.readValue(sampleCreateAccountEvent(), AccountActiveEvent.class);
    assertThat(event.getEventType()).isEqualTo(EventType.ACCOUNT_ACTIVE);
    assertThat(event.getAccount()).isNotNull();
    assertThat(event.getAccount().getId()).isEqualTo("E-AA4E-2AF3-EFDD-96D6");

    BaseEvent baseMessage = mapper.readValue(sampleCreateAccountEvent(), BaseEvent.class);
    assertThat(baseMessage.getEventType()).isEqualTo(EventType.ACCOUNT_ACTIVE);
    assertThat(baseMessage.getEventId()).isEqualTo("CREATE_ACCOUNT-3453c108-1ee5-474c-8905-683c1d4ce002");
  }

  private String sampleCreateAccountEvent() {
    return "{\n"
        + "  \"eventId\": \"CREATE_ACCOUNT-3453c108-1ee5-474c-8905-683c1d4ce002\",\n"
        + "  \"eventType\": \"ACCOUNT_ACTIVE\",\n"
        + "  \"account\": {\n"
        + "    \"id\": \"E-AA4E-2AF3-EFDD-96D6\",\n"
        + "    \"updateTime\": \"2019-06-12T18:55:24.707Z\"\n"
        + "  }\n"
        + "}";
  }
}