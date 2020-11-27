package io.harness.cvng.state;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.io.IOException;
import okhttp3.Request;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Call;
import retrofit2.Response;

public class CVNGRequestExecutorTest extends WingsBaseTest {
  @Inject CVNGRequestExecutor requestExecutor;

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_whenSuccess() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    String responseStr = "This is test response";
    Response<String> response = Response.success(responseStr);
    when(call.execute()).thenReturn(response);
    String returnedStr = requestExecutor.execute(call);
    assertThat(returnedStr).isEqualTo(responseStr);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_whenFailsWithIOException() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    IOException ioException = new IOException("io exception");
    when(call.execute()).thenThrow(ioException);
    assertThatThrownBy(() -> requestExecutor.execute(call))
        .isInstanceOf(IllegalStateException.class)
        .hasCause(ioException);
  }
}