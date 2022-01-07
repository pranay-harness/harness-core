/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.annotation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.annotation.ElementType.FIELD;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
FieldMap is converted to a hash value which is used to set the name for
InfrastructureMappings(software.wings.service.impl.infrastructuredefinition.InfrastructureDefinitionHelper.getNameFromInfraDefinition).
In case the fields are not simple strings for eg. map with re-orderable entries can produce different keys although they
are the same
 */
@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public @interface IncludeFieldMap {}
