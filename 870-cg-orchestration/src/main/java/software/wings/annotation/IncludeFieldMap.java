/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.annotation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.annotation.ElementType.FIELD;

import io.harness.annotations.dev.OwnedBy;

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
public @interface IncludeFieldMap {}
