package io.harness.batch.processing;

import static io.harness.event.app.EventServiceApplication.EVENTS_STORE;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.VerifyException;

import io.harness.CategoryTest;
import io.harness.annotation.StoreIn;
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
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
        .isInstanceOf(VerifyException.class)
        .withFailMessage("Unable to connect to timescale db");
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
  public void shouldEnsureIndexForEventsStoreClassesOnly() throws Exception {
    AdvancedDatastore datastore = mock(AdvancedDatastore.class);
    doReturn(datastore).when(hPersistence).getDatastore(EVENTS_STORE);
    PowerMockito.spy(IndexManager.class);
    PowerMockito.doNothing().when(IndexManager.class, "ensureIndex", refEq(datastore), captor.capture());
    listener.ensureIndexForEventsStore();
    Morphia morphia = captor.getValue();
    assertThat(
        morphia.getMapper().getMappedClasses().stream().map(MappedClass::getClazz).filter(cls -> !cls.isInterface()))
        .allMatch(clazz -> clazz.getAnnotation(StoreIn.class).value().equals("events"));
  }
}
