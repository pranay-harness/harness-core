package io.harness.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Maps;

import io.harness.category.element.UnitTests;
import lombok.Data;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

public class MapperUtilsTest {
  @Test
  @Category(UnitTests.class)
  public void mapObject() throws Exception {
    Map<String, Object> map = Maps.newLinkedHashMap();
    map.put("toAddress", "a@b.com");
    map.put("subject", "test");
    map.put("body", "test");

    EmailState emailState = new EmailState("id");
    MapperUtils.mapObject(map, emailState);
    assertThat(emailState)
        .extracting(EmailState::getToAddress, EmailState::getSubject, EmailState::getBody, EmailState::getName)
        .containsExactly("a@b.com", "test", "test", "id");
  }

  @Test
  @Category(UnitTests.class)
  public void mapSomeFields() throws Exception {
    EmailState emailState = new EmailState("name1");
    emailState.setBody("body1");

    Map<String, Object> map = new HashMap<>();
    map.put("toAddress", "toAddress1");
    map.put("ccAddress", "ccAddress1");

    MapperUtils.mapObject(map, emailState);

    assertThat(emailState)
        .extracting("name", "body", "toAddress", "ccAddress", "subject")
        .containsExactly("name1", "body1", "toAddress1", "ccAddress1", null);
  }

  @Data
  public class EmailState {
    private String name;
    private String toAddress;
    private String ccAddress;
    private String subject;
    private String body;
    private Boolean ignoreDeliveryFailure = true;

    public EmailState(String name) {
      this.name = name;
    }
  }
}
