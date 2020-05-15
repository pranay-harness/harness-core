package io.harness.batch.processing;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.google.common.base.VerifyException;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.mongo.IndexManager;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Morphia;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Duration;

@RunWith(PowerMockRunner.class)
@PrepareForTest(IndexManager.class)
public class ApplicationReadyListenerTest extends CategoryTest {
  @InjectMocks private ApplicationReadyListener listener;

  @Mock private HPersistence hPersistence;
  @Mock private TimeScaleDBService timeScaleDBService;

  @Captor private ArgumentCaptor<Morphia> captor;

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldFailIfTsdbNotConnectable() throws Exception {
    doReturn(false).when(timeScaleDBService).isValid();
    assertThatThrownBy(() -> listener.ensureTimescaleConnectivity())
        .withFailMessage("Unable to connect to timescale db")
        .isInstanceOf(VerifyException.class);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPassIfTsdbConnectable() throws Exception {
    doReturn(true).when(timeScaleDBService).isValid();
    assertThatCode(() -> listener.ensureTimescaleConnectivity()).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldFailIfMongoConnectivityRuntimeError() throws Exception {
    doReturn(Duration.ofSeconds(5)).when(hPersistence).healthExpectedResponseTimeout();
    doThrow(new RuntimeException("unknown")).when(hPersistence).isHealthy();
    assertThatThrownBy(() -> listener.ensureMongoConnectivity())
        .withFailMessage("unknown")
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldFailIfMongoConnectivityTimeoutError() throws Exception {
    doReturn(Duration.ofSeconds(5)).when(hPersistence).healthExpectedResponseTimeout();
    doThrow(new UncheckedTimeoutException("timed out")).when(hPersistence).isHealthy();
    assertThatThrownBy(() -> listener.ensureMongoConnectivity())
        .withFailMessage("timed out")
        .isInstanceOf(UncheckedTimeoutException.class);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPassIfMongoConnectivityDoesNotThrow() throws Exception {
    doReturn(Duration.ofSeconds(5)).when(hPersistence).healthExpectedResponseTimeout();
    doNothing().when(hPersistence).isHealthy();
    assertThatCode(() -> listener.ensureMongoConnectivity()).doesNotThrowAnyException();
  }
}
