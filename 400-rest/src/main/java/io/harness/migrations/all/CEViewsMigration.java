package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.views.dao.CEViewDao;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewCondition;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.entities.ViewTimeRange;
import io.harness.ccm.views.entities.ViewTimeRangeType;
import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@TargetModule(Module._390_DB_MIGRATION)
public class CEViewsMigration implements Migration {
  private final WingsPersistence wingsPersistence;
  private final CEViewDao ceViewDao;

  @Inject
  public CEViewsMigration(WingsPersistence wingsPersistence, CEViewDao ceViewDao) {
    this.wingsPersistence = wingsPersistence;
    this.ceViewDao = ceViewDao;
  }

  @Override
  public void migrate() {
    try {
      log.info("Starting migration (updates) of all CE Views");

      List<CEView> ceViewList = wingsPersistence.createQuery(CEView.class, excludeValidate).asList();
      for (CEView ceView : ceViewList) {
        try {
          migrateCEView(ceView);
        } catch (Exception e) {
          log.info("Migration Failed for Account {}, ViewId {}", ceView.getAccountId(), ceView.getUuid());
        }
      }
    } catch (Exception e) {
      log.error("Failure occurred in CEViewsMigration", e);
    }
    log.info("CEViewsMigration has completed");
  }

  private void migrateCEView(CEView ceView) {
    ceView.setViewTimeRange(ViewTimeRange.builder().viewTimeRangeType(ViewTimeRangeType.LAST_7).build());

    Set<ViewFieldIdentifier> viewFieldIdentifierSet = new HashSet<>();
    if (ceView.getViewRules() != null) {
      for (ViewRule rule : ceView.getViewRules()) {
        for (ViewCondition condition : rule.getViewConditions()) {
          if (((ViewIdCondition) condition).getViewField().getIdentifier() == ViewFieldIdentifier.CLUSTER) {
            viewFieldIdentifierSet.add(ViewFieldIdentifier.CLUSTER);
          }
          if (((ViewIdCondition) condition).getViewField().getIdentifier() == ViewFieldIdentifier.AWS) {
            viewFieldIdentifierSet.add(ViewFieldIdentifier.AWS);
          }
          if (((ViewIdCondition) condition).getViewField().getIdentifier() == ViewFieldIdentifier.GCP) {
            viewFieldIdentifierSet.add(ViewFieldIdentifier.GCP);
          }
          if (((ViewIdCondition) condition).getViewField().getIdentifier() == ViewFieldIdentifier.CUSTOM) {
            viewFieldIdentifierSet.add(ViewFieldIdentifier.CUSTOM);
          }
        }
      }
    }

    List<ViewFieldIdentifier> viewFieldIdentifierList = new ArrayList<>();
    viewFieldIdentifierList.addAll(viewFieldIdentifierSet);
    ceView.setDataSources(viewFieldIdentifierList);

    ceViewDao.update(ceView);
  }
}
