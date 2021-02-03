package io.harness.cvng.core.entities.cvnglogs;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.cvng.core.entities.cvnglogs.ApiCallLogRecord.ApiCallLogField;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiCallLogRecordTest {
  private String name;
  private String value;

  @Before
  public void setup() {
    this.name = generateUuid();
    this.value = generateUuid();
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testAddFieldToRequest_withNullField() {
    ApiCallLogRecord apiCallLogRecord = ApiCallLogRecord.builder().build();
    assertThatThrownBy(() -> apiCallLogRecord.addFieldToRequest(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Api call log request field is null.");
  }

  @Test()
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testAddFieldToRequest_withEmptyRequest() {
    ApiCallLogRecord apiCallLogRecord = ApiCallLogRecord.builder().build();
    ApiCallLogField field = ApiCallLogField.builder().name(name).value(value).build();
    apiCallLogRecord.addFieldToRequest(field);
    assertThat(apiCallLogRecord.getRequests()).hasSize(1);
    assertThat(apiCallLogRecord.getRequests().get(0).getName()).isEqualTo(name);
    assertThat(apiCallLogRecord.getRequests().get(0).getValue()).isEqualTo(value);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testAddFieldToResponse_withNullField() {
    ApiCallLogRecord apiCallLogRecord = ApiCallLogRecord.builder().build();
    assertThatThrownBy(() -> apiCallLogRecord.addFieldToResponse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Api call log response field is null.");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testAddFieldToResponse_withEmptyResponse() {
    ApiCallLogRecord apiCallLogRecord = ApiCallLogRecord.builder().build();
    ApiCallLogField field = ApiCallLogField.builder().name(name).value(value).build();
    apiCallLogRecord.addFieldToResponse(field);
    assertThat(apiCallLogRecord.getResponses()).hasSize(1);
    assertThat(apiCallLogRecord.getResponses().get(0).getName()).isEqualTo(name);
    assertThat(apiCallLogRecord.getResponses().get(0).getValue()).isEqualTo(value);
  }
}
