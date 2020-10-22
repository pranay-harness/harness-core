package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class TrialSignupOptions {
  @Getter @Setter private List<Products> productsSelected = new ArrayList<>();

  @Getter @Setter private Boolean assistedOption;

  public void populateProducts(List<String> freemiumProducts) {
    if (freemiumProducts == null) {
      return;
    }

    List<Products> products = new ArrayList<>();

    if (freemiumProducts.contains("CD - Continuous Delivery") || freemiumProducts.contains("CD")) {
      products.add(Products.CD);
    }
    if (freemiumProducts.contains("CE - Continuous Efficiency") || freemiumProducts.contains("CE")) {
      products.add(Products.CE);
    }
    if (freemiumProducts.contains("CI - Continuous Integration") || freemiumProducts.contains("CI")) {
      products.add(Products.CI);
    }

    this.productsSelected = products;
  }

  public enum Products { CD, CE, CI }
}
