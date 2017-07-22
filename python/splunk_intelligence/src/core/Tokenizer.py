import re

import nltk

nltk.download('punkt')

"""
Tokenize text to build feature vectors. Suports tokenizing based on NLTK.
"""
class Tokenizer(object):


    @staticmethod
    def tokenize_line_only(text):
        """
        Split each line into a token

        :param text: input
        :return: tokens extracted
        """
        return [s.strip() for s in text.splitlines()]


    @staticmethod
    def default_tokenizer(text):
        """
        Uses nltk sentence and word tokenizer
        Drops tokens less of size 3 or lower
        Keeps alpha numeric with dot, and throws the rest

        :param text: input
        :return: tokens extracted
        """
        # first tokenize by sentence, then by word to ensure that punctuation is caught as it's own token
        tokens = [word.lower() for sent in nltk.sent_tokenize(text) for word in nltk.word_tokenize(sent)]
        filtered_tokens = []
        # filter out any tokens not containing letters (e.g., numeric tokens, raw punctuation)
        # filter out any tokens the contain punctuation other than dot(.), underscore(_), equals(=)
        # filter out any tokens less that 4 characters
        for token in tokens:
            if len(token) > 3 and not re.search('[^0-9a-zA-Z._=]', token) \
                    and re.search('[a-zA-Z]', token) and token:
                filtered_tokens.append(token)
        if len(filtered_tokens) == 0:
            filtered_tokens.append(text)

        return filtered_tokens


# tokens1 = Tokenizer.default_tokenizer(''' 2017-07-20 22:51:45,657 \x1b[32m[dw-4434 - POST /api/settings?&accountId=kmpySmUISimoRrJL6NL73w]\x1b[0;39m \x1b[1;31mERROR\x1b[0;39m \x1b[36msoftware.wings.exception.WingsExceptionMapper\x1b[0;39m - ResponseMessage{code=INVALID_ARTIFACT_SERVER, errorType=ERROR, message=Jenkins URL must be a valid URL ''')
# print(tokens1)
# tokens2 = Tokenizer.default_tokenizer(''' 2017-07-20 22:48:57,067 \x1b[32m[dw-4435 - POST /api/settings?&accountId=kmpySmUISimoRrJL6NL73w]\x1b[0;39m \x1b[1;31mERROR\x1b[0;39m \x1b[36msoftware.wings.exception.WingsExceptionMapper\x1b[0;39m - Exception occurred: APPDYNAMICS_CONFIGURATION_ERROR \nsoftware.wings.exception.WingsException: APPDYNAMICS_CONFIGURATION_ERROR\n\tat software.wings.service.impl.appdynamics.AppdynamicsServiceImpl.validateConfig(AppdynamicsServiceImpl.java:114)\n\tat ru.vyarus.guice.validator.aop.ValidationMethodInterceptor.invoke(ValidationMethodInterceptor.java:52)\n\tat software.wings.service.impl.SettingValidationService.validate(SettingValidationService.java:48)\n\tat software.wings.service.impl.SettingsServiceImpl.save(SettingsServiceImpl.java:72)\n\tat ru.vyarus.guice.validator.aop.ValidationMethodInterceptor.invoke(ValidationMethodInterceptor.java:52)\n\tat software.wings.resources.SettingResource.save(SettingResource.java:106)\n\tat sun.reflect.GeneratedMethodAccessor139.invoke(Unknown Source)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:498)\n\tat org.glassfish.jersey.server.model.internal.ResourceMethodInvocationHandlerFactory$1.invoke(ResourceMethodInvocationHandlerFactory.java:81)\n\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher$1.run(AbstractJavaResourceMethodDispatcher.java:144)\n\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.invoke(AbstractJavaResourceMethodDispatcher.java:161)\n\tat org.glassfish.jersey.server.model.internal.JavaResourceMethodDispatcherProvider$TypeOutInvoker.doDispatch(JavaResourceMethodDispatcherProvider.java:205)\n\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.dispatch(AbstractJavaResourceMethodDispatcher.java:99)\n\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:389)\n\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:347)\n\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)\n\tat org.glassfish.jersey.server.ServerRuntime$2.run(ServerRuntime.java:326)\n\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)\n\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:315)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:297)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:267)\n\tat org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:317)\n\tat org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:305)\n\tat org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:1154)\n\tat org.glassfish.jersey.servlet.WebComponent.serviceImpl(WebComponent.java:473)\n\tat org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:427)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:388)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:341)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:228)\n\tat io.dropwizard.jetty.NonblockingServletHolder.handle(NonblockingServletHolder.java:49)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1689)\n\tat io.dropwizard.servlets.ThreadNameFilter.doFilter(ThreadNameFilter.java:34)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat io.dropwizard.jersey.filter.AllowedMethodsFilter.handle(AllowedMethodsFilter.java:50)\n\tat io.dropwizard.jersey.filter.AllowedMethodsFilter.doFilter(AllowedMethodsFilter.java:44)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat software.wings.filter.AuditResponseFilter.doFilter(AuditResponseFilter.java:55)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat org.eclipse.jetty.servlets.CrossOriginFilter.handle(CrossOriginFilter.java:308)\n\tat org.eclipse.jetty.servlets.CrossOriginFilter.doFilter(CrossOriginFilter.java:262)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:581)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1174)\n\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:511)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1106)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:141)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:134)\n\tat com.codahale.metrics.jetty9.InstrumentedHandler.handle(InstrumentedHandler.java:240)\n\tat io.dropwizard.jetty.RoutingHandler.handle(RoutingHandler.java:51)\n\tat org.eclipse.jetty.server.handler.gzip.GzipHandler.handle(GzipHandler.java:396)\n\tat io.dropwizard.jetty.BiDiGzipHandler.handle(BiDiGzipHandler.java:68)\n\tat org.eclipse.jetty.server.handler.RequestLogHandler.handle(RequestLogHandler.java:56)\n\tat org.eclipse.jetty.server.handler.StatisticsHandler.handle(StatisticsHandler.java:169)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:134)\n\tat org.eclipse.jetty.server.Server.handle(Server.java:524)\n\tat org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:319)\n\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:253)\n\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:273)\n\tat org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:95)\n\tat org.eclipse.jetty.io.SelectChannelEndPoint$2.run(SelectChannelEndPoint.java:93)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.executeProduceConsume(ExecuteProduceConsume.java:303)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.produceConsume(ExecuteProduceConsume.java:148)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.run(ExecuteProduceConsume.java:136)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:671)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool$2.run(QueuedThreadPool.java:589)\n\tat java.lang.Thread.run(Thread.java:748) ''')
# print(tokens2)
#
# print(len(set(tokens1).intersection(set(tokens2))))
# print(len(set(tokens1).union(set(tokens2))))
#
#
# intersection = set(tokens1).intersection(set(tokens2))
# union = set(tokens1).union(set(tokens2))
# print (float(len(intersection)) / len(union))
