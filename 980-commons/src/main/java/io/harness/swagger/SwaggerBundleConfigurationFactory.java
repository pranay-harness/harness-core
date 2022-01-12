/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.swagger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.Contact;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

@UtilityClass
public class SwaggerBundleConfigurationFactory {
  public SwaggerBundleConfiguration buildSwaggerBundleConfiguration(Collection<Class<?>> resourceClasses) {
    return new SwaggerBundleConfiguration() {
      @JsonIgnore
      public BeanConfig build(String urlPattern) {
        final BeanConfig config = new BeanConfig() {
          @Override
          public Set<Class<?>> classes() {
            ConfigurationBuilder config = new ConfigurationBuilder();
            Set<String> acceptablePackages = new HashSet<>();
            boolean allowAllPackages = false;
            final FilterBuilder filter = new FilterBuilder();
            if (getResourcePackage() != null && !"".equals(getResourcePackage())) {
              String[] parts = getResourcePackage().split(",");
              for (String pkg : parts) {
                if (!"".equals(pkg)) {
                  acceptablePackages.add(pkg);
                  filter.includePackage(pkg);
                  config.addUrls(ClasspathHelper.forPackage(pkg));
                }
              }
            } else {
              allowAllPackages = true;
            }
            config.setExpandSuperTypes(getExpandSuperTypes());
            config.filterInputsBy(filter);
            config.setScanners(new ResourcesScanner(), new TypeAnnotationsScanner(), new SubTypesScanner());
            Set<Class<?>> classes = new HashSet<>(resourceClasses);

            /*
             * Find concrete types annotated with @Api, but with a supertype annotated with @Path.
             * This would handle split resources where the interface has jax-rs annotations
             * and the implementing class has Swagger annotations
             */

            Set<Class<?>> output = new HashSet<>();
            for (Class<?> cls : classes) {
              if (allowAllPackages) {
                output.add(cls);
              } else {
                for (String pkg : acceptablePackages) {
                  // startsWith allows everything within a package
                  // the dots ensures that package siblings are not considered
                  if ((cls.getPackage().getName() + ".").startsWith(pkg + ".")) {
                    output.add(cls);
                    break;
                  }
                }
              }
            }
            return output;
          }

          @Override
          public void setScan(boolean shouldScan) {
            new SwaggerContextService()
                .withConfigId(this.getConfigId())
                .withScannerId(this.getScannerId())
                .withContextId(this.getContextId())
                .withServletConfig(this.servletConfig)
                .withSwaggerConfig(this)
                .withScanner(this)
                .withBasePath(getBasePath())
                .withPathBasedConfig(isUsePathBasedConfig())
                .initConfig()
                .initScanner();
          }
        };
        config.setTitle(getTitle());
        config.setVersion(getVersion());
        config.setDescription(getDescription());
        config.setContact(getContact());
        config.setLicense(getLicense());
        config.setLicenseUrl(getLicenseUrl());
        config.setTermsOfServiceUrl(getTermsOfServiceUrl());
        config.setPrettyPrint(isPrettyPrint());
        config.setBasePath(("/".equals(getContextRoot()) ? "" : getContextRoot()) + urlPattern);
        config.setResourcePackage(getResourcePackage());
        config.setSchemes(getSchemes());
        config.setHost(getHost());
        config.setScan(true);

        // Assign contact email/url after scan, since BeanConfig.scan will
        // create a new info.Contact instance, thus overriding any info.Contact
        // settings prior to scan.
        if (getContactEmail() != null || getContactUrl() != null) {
          if (config.getInfo().getContact() == null) {
            config.getInfo().setContact(new Contact());
          }
          if (getContactEmail() != null) {
            config.getInfo().getContact().setEmail(getContactEmail());
          }
          if (getContactUrl() != null) {
            config.getInfo().getContact().setUrl(getContactUrl());
          }
        }

        return config;
      }
    };
  }
}
