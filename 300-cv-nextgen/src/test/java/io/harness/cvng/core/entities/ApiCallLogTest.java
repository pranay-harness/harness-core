package io.harness.cvng.core.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.entities.ApiCallLog.ApiCallLogField;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiCallLogTest extends CvNextGenTest {
  private String accountId;
  private String name;
  private String value;

  @Before
  public void setup() {
    this.accountId = generateUuid();
    this.name = generateUuid();
    this.value = generateUuid();
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testAddFieldToRequest_NullField() {
    ApiCallLog apiCallLog = ApiCallLog.builder().accountId(accountId).build();
    assertThatThrownBy(() -> apiCallLog.addFieldToRequest(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Api call log request field is null.");
  }

  @Test()
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testAddFieldToRequest_EmptyRequest() {
    ApiCallLog apiCallLog = ApiCallLog.builder().accountId(accountId).build();
    ApiCallLogField field = ApiCallLogField.builder().name(name).value(value).build();
    apiCallLog.addFieldToRequest(field);
    assertThat(apiCallLog.getRequests()).hasSize(1);
    assertThat(apiCallLog.getRequests().get(0).getName()).isEqualTo(name);
    assertThat(apiCallLog.getRequests().get(0).getValue()).isEqualTo(value);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testAddFieldToResponse_NullField() {
    ApiCallLog apiCallLog = ApiCallLog.builder().accountId(accountId).build();
    assertThatThrownBy(() -> apiCallLog.addFieldToResponse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Api call log response field is null.");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testAddFieldToResponse_EmptyResponse() {
    ApiCallLog apiCallLog = ApiCallLog.builder().accountId(accountId).build();
    ApiCallLogField field = ApiCallLogField.builder().name(name).value(value).build();
    apiCallLog.addFieldToResponse(field);
    assertThat(apiCallLog.getResponses()).hasSize(1);
    assertThat(apiCallLog.getResponses().get(0).getName()).isEqualTo(name);
    assertThat(apiCallLog.getResponses().get(0).getValue()).isEqualTo(value);
  }
}
