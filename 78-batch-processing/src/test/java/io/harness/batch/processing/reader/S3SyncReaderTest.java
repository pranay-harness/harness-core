package io.harness.batch.processing.reader;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.SettingAttribute;

import java.util.concurrent.atomic.AtomicBoolean;

public class S3SyncReaderTest {
  @Inject @InjectMocks private S3SyncReader s3SyncReader;
  @Mock AtomicBoolean runOnlyOnce;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    runOnlyOnce = new AtomicBoolean(false);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testS3SyncRead() {
    SettingAttribute settingAttribute = s3SyncReader.read();
    assertThat(settingAttribute).isNotNull();
    SettingAttribute secondReadOutput = s3SyncReader.read();
    assertThat(secondReadOutput).isNull();
  }
}