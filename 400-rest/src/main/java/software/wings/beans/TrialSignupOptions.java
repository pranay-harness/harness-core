/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class TrialSignupOptions {
  @Getter @Setter private List<Products> productsSelected = new ArrayList<>();

  @Getter @Setter private Boolean assistedOption;

  public TrialSignupOptions(List<Products> freemiumProducts, Boolean assistedOption) {
    if (isEmpty(freemiumProducts)) {
      this.productsSelected = getDefaultProducts();
    } else {
      this.productsSelected = freemiumProducts;
    }

    if (assistedOption == null) {
      this.assistedOption = true;
    } else {
      this.assistedOption = assistedOption;
    }
  }

  private static List<Products> getDefaultProducts() {
    return Arrays.asList(Products.CD, Products.CE, Products.CI);
  }

  public static TrialSignupOptions getDefaultTrialSignupOptions() {
    TrialSignupOptions trialSignupOptions = new TrialSignupOptions();
    trialSignupOptions.setAssistedOption(true);
    trialSignupOptions.setProductsSelected(getDefaultProducts());
    return trialSignupOptions;
  }

  public enum Products {
    CD("CD - Continuous Delivery"),
    CE("CE - Continuous Efficiency"),
    CI("CI - Continuous Integration");

    @Getter private final String fullName;

    Products(String fullName) {
      this.fullName = fullName;
    }

    public static List<Products> getProductsFromFullNames(List<String> fullNames) {
      if (isEmpty(fullNames)) {
        return Collections.emptyList();
      }

      return Stream.of(Products.values())
          .filter(product -> fullNames.contains(product.getFullName()))
          .collect(Collectors.toList());
    }
  }
}
