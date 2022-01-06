/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.SOWMYA;
import static io.harness.rule.OwnerRule.UNKNOWN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.Misc;
import io.harness.version.ServiceApiVersion;

import software.wings.utils.NoFieldShadowingRule;
import software.wings.utils.ToStringTester;

import com.openpojo.reflection.PojoClassFilter;
import com.openpojo.reflection.filters.FilterClassName;
import com.openpojo.reflection.filters.FilterEnum;
import com.openpojo.reflection.filters.FilterNestedClasses;
import com.openpojo.reflection.filters.FilterNonConcrete;
import com.openpojo.reflection.filters.FilterPackageInfo;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.NoPublicFieldsExceptStaticFinalRule;
import com.openpojo.validation.rule.impl.SerializableMustHaveSerialVersionUIDRule;
import com.openpojo.validation.rule.impl.TestClassMustBeProperlyNamedRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by peeyushaggarwal on 5/18/16.
 */
public class MiscellaneousTest extends CategoryTest {
  private static final Pattern NO_TEST_PATTERN = Pattern.compile("^((?!test-classes/$).)*$");
  private static final Pattern EXCLUDE_APP_PACKAGE = Pattern.compile("^((?!software\\.wings\\.app).)*$");
  private static final Pattern EXCLUDE_BEANS_PACKAGE = Pattern.compile("^((?!software\\.wings\\.beans).)*$");
  private static final Pattern EXCLUDE_DL_PACKAGE = Pattern.compile("^((?!software\\.wings\\.dl).)*$");

  private static final PojoClassFilter NO_TEST_FILTER = pojoClass
      -> NO_TEST_PATTERN.matcher(pojoClass.getClazz().getProtectionDomain().getCodeSource().getLocation().getPath())
             .find();

  private static final PojoClassFilter TEST_ONLY_FILTER = pojoClass
      -> !NO_TEST_PATTERN.matcher(pojoClass.getClazz().getProtectionDomain().getCodeSource().getLocation().getPath())
              .find();

  private static final PojoClassFilter EXCLUDE_PACKAGES_FOR_GETTER_SETTER_TO_STRING = pojoClass
      -> !EXCLUDE_APP_PACKAGE.matcher(pojoClass.getClazz().getName()).find()
      && !EXCLUDE_BEANS_PACKAGE.matcher(pojoClass.getClazz().getName()).find()
      && !EXCLUDE_DL_PACKAGE.matcher(pojoClass.getClazz().getName()).find();

  private static final PojoClassFilter[] classFilters = {new FilterNonConcrete(), new FilterNestedClasses(),
      new FilterEnum(), new FilterPackageInfo(), NO_TEST_FILTER, EXCLUDE_PACKAGES_FOR_GETTER_SETTER_TO_STRING,
      new FilterClassName("^((?!Test$).)*$"), new FilterClassName("^((?!Tester$).)*$")};

  private static final PojoClassFilter EXCLUDE_APP_PACKAGE_FILTER =
      pojoClass -> !EXCLUDE_APP_PACKAGE.matcher(pojoClass.getClazz().getName()).find();

  /**
   * Should provide coverage to getter setter and to string.
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldProvideCoverageToGetterSetterAndToString() {
    Validator validator =
        ValidatorBuilder.create().with(new SetterTester()).with(new GetterTester()).with(new ToStringTester()).build();

    validator.validateRecursively("software.wings", classFilters);
  }

  /**
   * Should validate code governance rules.
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldValidateCodeGovernanceRules() {
    Validator validator = ValidatorBuilder.create()
                              .with(new NoPublicFieldsExceptStaticFinalRule())
                              .with(new SerializableMustHaveSerialVersionUIDRule())
                              .with(new NoFieldShadowingRule())
                              .build();

    validator.validateRecursively(
        "software.wings", EXCLUDE_APP_PACKAGE_FILTER, NO_TEST_FILTER, new FilterEnum(), new FilterNonConcrete());
  }

  /**
   * Should validate test governance rules.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldValidateTestGovernanceRules() {
    Validator validator = ValidatorBuilder.create().with(new TestClassMustBeProperlyNamedRule()).build();

    validator.validateRecursively("software.wings", TEST_ONLY_FILTER, new FilterClassName("^((?!Utils$).)*$"));
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testParseApisVersion() {
    ServiceApiVersion latestVersion = ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];
    assertThat(Misc.parseApisVersion("application/json")).isEqualTo(latestVersion);

    for (ServiceApiVersion serviceApiVersion : ServiceApiVersion.values()) {
      String headerString = "application/" + serviceApiVersion.name().toLowerCase() + "+json, application/json";
      assertThat(Misc.parseApisVersion(headerString)).isEqualTo(serviceApiVersion);
    }
  }
}
