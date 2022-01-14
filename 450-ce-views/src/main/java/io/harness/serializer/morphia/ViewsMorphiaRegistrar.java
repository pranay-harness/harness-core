package io.harness.serializer.morphia;

import io.harness.ccm.views.businessMapping.entities.BusinessMapping;
import io.harness.ccm.views.entities.CEReportSchedule;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class ViewsMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(CEView.class);
    set.add(ViewCustomField.class);
    set.add(CEReportSchedule.class);
    set.add(BusinessMapping.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
