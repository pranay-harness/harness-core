package io.harness.serializer;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Maps;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

public class MapperUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
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
  @Owner(developers = GEORGE)
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

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void mapObjectOnlyNonNull() {
    EmailState emailState1 = new EmailState("name1", "toAddress1", "ccAddress1", "subject1", "body1", true);

    EmailState emailState2 = new EmailState("name2", "toAddress2", "ccAddress2", null, null, true);

    MapperUtils.mapObjectOnlyNonNull(emailState2, emailState1);

    assertThat(emailState1)
        .extracting("name", "body", "toAddress", "ccAddress", "subject")
        .containsExactly("name2", "body1", "toAddress2", "ccAddress2", "subject1");
  }

  @Data
  @AllArgsConstructor
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
