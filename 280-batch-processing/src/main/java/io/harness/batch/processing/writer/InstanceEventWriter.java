package io.harness.batch.processing.writer;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("instanceEventWriter")
public class InstanceEventWriter implements ItemWriter<InstanceEvent> {
  @Autowired protected InstanceDataDao instanceDataDao;

  @Override
  public void write(List<? extends InstanceEvent> instanceEvents) throws Exception {
    instanceEvents.forEach(instanceEvent -> instanceDataDao.upsert(instanceEvent));
  }
}
