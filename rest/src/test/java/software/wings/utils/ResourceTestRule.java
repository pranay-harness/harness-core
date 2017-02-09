package software.wings.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.validation.Validators;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletProperties;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import software.wings.exception.ConstraintViolationExceptionMapper;
import software.wings.jersey.KryoMessageBodyProvider;

import java.util.Map;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.validation.Validator;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Context;

/**
 * Created by peeyushaggarwal on 6/16/16.
 */
public class ResourceTestRule implements TestRule {
  private final Set<Object> singletons;
  private final Set<Class<?>> providers;
  private final Map<String, Object> properties;
  private final ObjectMapper mapper;
  private final Validator validator;
  private final TestContainerFactory testContainerFactory;
  private JerseyTest test;

  private ResourceTestRule(Set<Object> singletons, Set<Class<?>> providers, Map<String, Object> properties,
      ObjectMapper mapper, Validator validator, TestContainerFactory testContainerFactory) {
    this.singletons = singletons;
    this.providers = providers;
    this.properties = properties;
    this.mapper = mapper;
    this.validator = validator;
    this.testContainerFactory = testContainerFactory;
  }

  /**
   * Builder builder.
   *
   * @return the builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets validator.
   *
   * @return the validator
   */
  public Validator getValidator() {
    return validator;
  }

  /**
   * Gets object mapper.
   *
   * @return the object mapper
   */
  public ObjectMapper getObjectMapper() {
    return mapper;
  }

  /**
   * Client client.
   *
   * @return the client
   */
  public Client client() {
    return test.client();
  }

  /**
   * Gets jersey test.
   *
   * @return the jersey test
   */
  public JerseyTest getJerseyTest() {
    return test;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    final ResourceTestRule rule = this;
    final String ruleId = String.valueOf(rule.hashCode());
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          test = new JerseyTest() {
            @Override
            protected TestContainerFactory getTestContainerFactory() {
              return testContainerFactory;
            }

            @Override
            protected DeploymentContext configureDeployment() {
              final ResourceTestResourceConfig resourceConfig = new ResourceTestResourceConfig(ruleId, rule);
              return ServletDeploymentContext.builder(resourceConfig)
                  .initParam(ServletProperties.JAXRS_APPLICATION_CLASS, ResourceTestResourceConfig.class.getName())
                  .initParam(ResourceTestResourceConfig.RULE_ID, ruleId)
                  .build();
            }

            @Override
            protected void configureClient(final ClientConfig config) {
              config.register(KryoMessageBodyProvider.class, 0);

              JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
              jsonProvider.setMapper(mapper);
              config.register(jsonProvider);
            }
          };
          test.setUp();
          base.evaluate();
        } finally {
          ResourceTestResourceConfig.RULE_ID_TO_RULE.remove(ruleId);
          if (test != null) {
            test.tearDown();
          }
        }
      }
    };
  }

  /**
   * The type Builder.
   */
  public static class Builder {
    private final Set<Object> singletons = Sets.newHashSet();
    private final Set<Class<?>> providers = Sets.newHashSet();
    private final Map<String, Object> properties = Maps.newHashMap();
    private ObjectMapper mapper = Jackson.newObjectMapper();
    private Validator validator = Validators.newValidator();
    private TestContainerFactory testContainerFactory = new InMemoryTestContainerFactory();

    /**
     * Sets mapper.
     *
     * @param mapper the mapper
     * @return the mapper
     */
    public Builder setMapper(ObjectMapper mapper) {
      this.mapper = mapper;
      return this;
    }

    /**
     * Sets validator.
     *
     * @param validator the validator
     * @return the validator
     */
    public Builder setValidator(Validator validator) {
      this.validator = validator;
      return this;
    }

    /**
     * Add resource builder.
     *
     * @param resource the resource
     * @return the builder
     */
    public Builder addResource(Object resource) {
      singletons.add(resource);
      return this;
    }

    /**
     * Add provider builder.
     *
     * @param klass the klass
     * @return the builder
     */
    public Builder addProvider(Class<?> klass) {
      providers.add(klass);
      return this;
    }

    /**
     * Add provider builder.
     *
     * @param provider the provider
     * @return the builder
     */
    public Builder addProvider(Object provider) {
      singletons.add(provider);
      return this;
    }

    /**
     * Add property builder.
     *
     * @param property the property
     * @param value    the value
     * @return the builder
     */
    public Builder addProperty(String property, Object value) {
      properties.put(property, value);
      return this;
    }

    /**
     * Sets test container factory.
     *
     * @param factory the factory
     * @return the test container factory
     */
    public Builder setTestContainerFactory(TestContainerFactory factory) {
      this.testContainerFactory = factory;
      return this;
    }

    /**
     * Build resource test rule.
     *
     * @return the resource test rule
     */
    public ResourceTestRule build() {
      return new ResourceTestRule(singletons, providers, properties, mapper, validator, testContainerFactory);
    }
  }

  /**
   * The type Resource test resource config.
   */
  public static class ResourceTestResourceConfig extends DropwizardResourceConfig {
    private static final String RULE_ID = "io.dropwizard.testing.junit.resourceTestRuleId";
    private static final Map<String, ResourceTestRule> RULE_ID_TO_RULE = Maps.newHashMap();

    /**
     * Instantiates a new Resource test resource config.
     *
     * @param ruleId           the rule id
     * @param resourceTestRule the resource test rule
     */
    public ResourceTestResourceConfig(final String ruleId, final ResourceTestRule resourceTestRule) {
      super(true, new MetricRegistry());
      RULE_ID_TO_RULE.put(ruleId, resourceTestRule);
      configure(resourceTestRule);
    }

    /**
     * Instantiates a new Resource test resource config.
     *
     * @param servletConfig the servlet config
     */
    public ResourceTestResourceConfig(@Context ServletConfig servletConfig) {
      super(true, new MetricRegistry());
      String ruleId = servletConfig.getInitParameter(RULE_ID);
      Preconditions.checkNotNull(ruleId);

      ResourceTestRule resourceTestRule = RULE_ID_TO_RULE.get(ruleId);
      Preconditions.checkNotNull(resourceTestRule);
      configure(resourceTestRule);
    }

    private void configure(final ResourceTestRule resourceTestRule) {
      register(new ConstraintViolationExceptionMapper());
      for (Class<?> provider : resourceTestRule.providers) {
        register(provider);
      }
      property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, "true");
      for (Map.Entry<String, Object> property : resourceTestRule.properties.entrySet()) {
        property(property.getKey(), property.getValue());
      }
      register(new JacksonMessageBodyProvider(resourceTestRule.mapper));
      for (Object singleton : resourceTestRule.singletons) {
        register(singleton);
      }
    }
  }
}
