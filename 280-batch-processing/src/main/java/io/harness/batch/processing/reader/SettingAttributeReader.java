package io.harness.batch.processing.reader;

import software.wings.beans.SettingAttribute;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SettingAttributeReader implements ItemReader<SettingAttribute> {
  private AtomicBoolean runOnlyOnce;

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    runOnlyOnce = new AtomicBoolean(false);
  }

  @Override
  public SettingAttribute read() {
    SettingAttribute settingAttribute = null;
    if (!runOnlyOnce.getAndSet(true)) {
      settingAttribute = new SettingAttribute();
    }
    return settingAttribute;
  }
}
