package software.wings.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class TrialSignupOptions {
  @Getter @Setter private List<Products> productsSelected = new ArrayList<>();

  @Getter @Setter private Boolean assistedOption;

  public void populateProducts(List<String> freemiumProducts) {
    List<Products> products = new ArrayList<>();

    if (freemiumProducts == null) {
      products = Arrays.asList(Products.CD, Products.CE, Products.CI);
    } else {
      if (freemiumProducts.contains("CD - Continuous Delivery") || freemiumProducts.contains("CD")) {
        products.add(Products.CD);
      }
      if (freemiumProducts.contains("CE - Continuous Efficiency") || freemiumProducts.contains("CE")) {
        products.add(Products.CE);
      }
      if (freemiumProducts.contains("CI - Continuous Integration") || freemiumProducts.contains("CI")) {
        products.add(Products.CI);
      }
    }

    this.productsSelected = products;
  }

  public void populateProducts() {
    this.productsSelected = Arrays.asList(Products.CD, Products.CE, Products.CI);
  }

  public enum Products { CD, CE, CI }
}
