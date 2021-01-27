package io.harness.ccm.anomaly.service.impl;

import io.harness.ccm.anomaly.dao.AnomalyEntityDao;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;

import com.google.inject.Inject;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnomalyServiceImpl implements AnomalyService {
  @Autowired @Inject private AnomalyEntityDao anomalyEntityDao;

  @Override
  public List<AnomalyEntity> list(String account, Instant date) {
    SelectQuery query = new SelectQuery();
    query.addAllTableColumns(AnomalyEntity.AnomaliesDataTableSchema.table);
    addAccountIdFilter(account, query);
    query.addCondition(
        BinaryCondition.equalTo(AnomalyEntity.AnomaliesDataTableSchema.anomalyTime, date.truncatedTo(ChronoUnit.DAYS)));
    return anomalyEntityDao.list(query.validate().toString());
  }

  @Override
  public void delete(List<String> ids, Instant date) {
    anomalyEntityDao.delete(ids, date);
  }

  private void addAccountIdFilter(String accountId, SelectQuery query) {
    query.addCondition(BinaryCondition.equalTo(AnomalyEntity.AnomaliesDataTableSchema.accountId, accountId));
  }
}
