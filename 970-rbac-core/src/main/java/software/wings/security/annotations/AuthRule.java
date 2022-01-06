/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.security.annotations;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.NameBinding;

/**
 * Created by anubhaw on 3/10/16.
 */
@OwnedBy(PL)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@NameBinding
@Repeatable(AuthRules.class)
public @interface AuthRule {
  /**
   * Scope permission permissionType.
   *
   * @return the permission permissionType
   */
  PermissionType permissionType() default PermissionType.NONE;

  Action action() default Action.DEFAULT;

  /**
   * If the query / path parameter has a different name other than the default name for the permission type.
   * @return
   */
  String parameterName() default "";

  boolean skipAuth() default false;

  String dbFieldName() default "";

  String dbCollectionName() default "";
}
