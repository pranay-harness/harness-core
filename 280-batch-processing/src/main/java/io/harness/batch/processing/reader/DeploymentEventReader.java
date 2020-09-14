package io.harness.batch.processing.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class DeploymentEventReader implements ItemReader<List<String>> {
  private AtomicBoolean runOnlyOnce;

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    runOnlyOnce = new AtomicBoolean(false);
  }

  @Override
  public List<String> read() {
    List<String> distinctInstanceIds = null;
    if (!runOnlyOnce.getAndSet(true)) {
      distinctInstanceIds = new ArrayList<>();
    }
    return distinctInstanceIds;
  }
}
