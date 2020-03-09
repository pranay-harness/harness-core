package io.harness.batch.processing.writer;

import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Qualifier("instanceInfoWriter")
public class InstanceInfoWriter implements ItemWriter<InstanceInfo> {
  @Autowired protected InstanceDataDao instanceDataDao;

  @Override
  public void write(List<? extends InstanceInfo> instanceInfoList) throws Exception {
    instanceInfoList.stream()
        .filter(instanceInfo -> instanceInfo.getMetaData().containsKey(InstanceMetaDataConstants.INSTANCE_CATEGORY))
        .forEach(instanceInfo -> instanceDataDao.upsert(instanceInfo));
  }
}
