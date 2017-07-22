import numpy as np
from Tokenizer import Tokenizer

"""
Computes the jaccard simmilarity between 2 boolean vectors

J(A,B) =      | A int B |
        ----------------------
        |A| + |B| - | A int B |
"""

def pairwise_jaccard_similarity(X):
    """

    :param X: m X n sparse matrix
    :return: jaccard similarity score for all rows of X
    """

    X = X.astype(bool).astype(float)

    intrsct = X.dot(X.T)
    row_sums = intrsct.diagonal()
    unions = row_sums[:, None] + row_sums - intrsct
    # dist = 1.0 - intrsct / unions
    return intrsct / unions


def jaccard_similarity(X, Y):
    """

    :param X: m X n sparse matrix
    :return: jaccard similarity score for all rows of X
    """

    X = X.astype(bool).astype(float)
    Y = Y.astype(bool).astype(float)

    intrsct = X.dot(Y.T)
    unions = np.repeat(np.sum(X, axis=1)[:,None], Y.shape[0], axis=1) + np.sum(Y, axis=1) - intrsct
    return intrsct / unions

#TODO pass tokenizer as argument
def jaccard_text_similarity(control_texts, test_text):
    test_tokens = Tokenizer.default_tokenizer(test_text)
    sim = []
    for text in control_texts:
        control_tokens = Tokenizer.default_tokenizer(text)
        intersection = set(control_tokens).intersection(set(test_tokens))
        union = set(control_tokens).union(set(test_tokens))
        sim.append(float(len(intersection)) / len(union))
    return np.array(sim)






# x = [[1.,1,1,1], [1,1,1,0]]
#
# print(pairwise_jaccard_similarity(np.array(x)))
# print(jaccard_similarity(np.array(x), np.array([[1.,1,1,1], [3.,3,3,3], [0.,0.,3,3]])))


# tokens1 = Tokenizer.default_tokenizer(
#     ''' 2017-07-21 22:26:58,967 \u001b[32m[dw-142 - POST /api/settings?&accountId=kmpySmUISimoRrJL6NL73w]\u001b[0;39m \u001b[1;31mERROR\u001b[0;39m \u001b[36msoftware.wings.exception.WingsExceptionMapper\u001b[0;39m - Exception occurred: INVALID_CLOUD_PROVIDER \nsoftware.wings.exception.WingsException: INVALID_CLOUD_PROVIDER\n\tat software.wings.service.impl.AwsHelperService.validateAwsAccountCredential(AwsHelperService.java:90)\n\tat software.wings.service.impl.SettingValidationService.validate(SettingValidationService.java:43)\n\tat software.wings.service.impl.SettingsServiceImpl.save(SettingsServiceImpl.java:72)\n\tat ru.vyarus.guice.validator.aop.ValidationMethodInterceptor.invoke(ValidationMethodInterceptor.java:52)\n\tat software.wings.resources.SettingResource.save(SettingResource.java:106)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:498)\n\tat org.glassfish.jersey.server.model.internal.ResourceMethodInvocationHandlerFactory$1.invoke(ResourceMethodInvocationHandlerFactory.java:81)\n\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher$1.run(AbstractJavaResourceMethodDispatcher.java:144)\n\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.invoke(AbstractJavaResourceMethodDispatcher.java:161)\n\tat org.glassfish.jersey.server.model.internal.JavaResourceMethodDispatcherProvider$TypeOutInvoker.doDispatch(JavaResourceMethodDispatcherProvider.java:205)\n\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.dispatch(AbstractJavaResourceMethodDispatcher.java:99)\n\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:389)\n\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:347)\n\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)\n\tat org.glassfish.jersey.server.ServerRuntime$2.run(ServerRuntime.java:326)\n\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)\n\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:315)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:297)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:267)\n\tat org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:317)\n\tat org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:305)\n\tat org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:1154)\n\tat org.glassfish.jersey.servlet.WebComponent.serviceImpl(WebComponent.java:473)\n\tat org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:427)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:388)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:341)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:228)\n\tat io.dropwizard.jetty.NonblockingServletHolder.handle(NonblockingServletHolder.java:49)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1689)\n\tat io.dropwizard.servlets.ThreadNameFilter.doFilter(ThreadNameFilter.java:34)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat io.dropwizard.jersey.filter.AllowedMethodsFilter.handle(AllowedMethodsFilter.java:50)\n\tat io.dropwizard.jersey.filter.AllowedMethodsFilter.doFilter(AllowedMethodsFilter.java:44)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat software.wings.filter.AuditResponseFilter.doFilter(AuditResponseFilter.java:55)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat org.eclipse.jetty.servlets.CrossOriginFilter.handle(CrossOriginFilter.java:308)\n\tat org.eclipse.jetty.servlets.CrossOriginFilter.doFilter(CrossOriginFilter.java:262)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:581)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1174)\n\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:511)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1106)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:141)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:134)\n\tat com.codahale.metrics.jetty9.InstrumentedHandler.handle(InstrumentedHandler.java:240)\n\tat io.dropwizard.jetty.RoutingHandler.handle(RoutingHandler.java:51)\n\tat org.eclipse.jetty.server.handler.gzip.GzipHandler.handle(GzipHandler.java:396)\n\tat io.dropwizard.jetty.BiDiGzipHandler.handle(BiDiGzipHandler.java:68)\n\tat org.eclipse.jetty.server.handler.RequestLogHandler.handle(RequestLogHandler.java:56)\n\tat org.eclipse.jetty.server.handler.StatisticsHandler.handle(StatisticsHandler.java:169)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:134)\n\tat org.eclipse.jetty.server.Server.handle(Server.java:524)\n\tat org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:319)\n\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:253)\n\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:273)\n\tat org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:95)\n\tat org.eclipse.jetty.io.SelectChannelEndPoint$2.run(SelectChannelEndPoint.java:93)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.executeProduceConsume(ExecuteProduceConsume.java:303)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.produceConsume(ExecuteProduceConsume.java:148)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.run(ExecuteProduceConsume.java:136)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:671)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool$2.run(QueuedThreadPool.java:589)\n\tat java.lang.Thread.run(Thread.java:748) ''')
# print(tokens1)
# tokens2 = Tokenizer.default_tokenizer(
#     ''' 2017-07-21 22:26:50,935 \u001b[32m[dw-4492 - PUT /api/users/cTCORVGnQziByb6autmDKQ]\u001b[0;39m \u001b[1;31mERROR\u001b[0;39m \u001b[36msoftware.wings.exception.WingsExceptionMapper\u001b[0;39m - Exception occurred: ACCESS_DENIED \nsoftware.wings.exception.WingsException: ACCESS_DENIED\n\tat software.wings.resources.UserResource.update(UserResource.java:157)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:498)\n\tat org.glassfish.jersey.server.model.internal.ResourceMethodInvocationHandlerFactory$1.invoke(ResourceMethodInvocationHandlerFactory.java:81)\n\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher$1.run(AbstractJavaResourceMethodDispatcher.java:144)\n\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.invoke(AbstractJavaResourceMethodDispatcher.java:161)\n\tat org.glassfish.jersey.server.model.internal.JavaResourceMethodDispatcherProvider$TypeOutInvoker.doDispatch(JavaResourceMethodDispatcherProvider.java:205)\n\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.dispatch(AbstractJavaResourceMethodDispatcher.java:99)\n\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:389)\n\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:347)\n\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)\n\tat org.glassfish.jersey.server.ServerRuntime$2.run(ServerRuntime.java:326)\n\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)\n\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:315)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:297)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:267)\n\tat org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:317)\n\tat org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:305)\n\tat org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:1154)\n\tat org.glassfish.jersey.servlet.WebComponent.serviceImpl(WebComponent.java:473)\n\tat org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:427)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:388)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:341)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:228)\n\tat io.dropwizard.jetty.NonblockingServletHolder.handle(NonblockingServletHolder.java:49)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1689)\n\tat io.dropwizard.servlets.ThreadNameFilter.doFilter(ThreadNameFilter.java:34)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat io.dropwizard.jersey.filter.AllowedMethodsFilter.handle(AllowedMethodsFilter.java:50)\n\tat io.dropwizard.jersey.filter.AllowedMethodsFilter.doFilter(AllowedMethodsFilter.java:44)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat software.wings.filter.AuditResponseFilter.doFilter(AuditResponseFilter.java:55)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat org.eclipse.jetty.servlets.CrossOriginFilter.handle(CrossOriginFilter.java:308)\n\tat org.eclipse.jetty.servlets.CrossOriginFilter.doFilter(CrossOriginFilter.java:262)\n\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1676)\n\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:581)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1174)\n\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:511)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1106)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:141)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:134)\n\tat com.codahale.metrics.jetty9.InstrumentedHandler.handle(InstrumentedHandler.java:240)\n\tat io.dropwizard.jetty.RoutingHandler.handle(RoutingHandler.java:51)\n\tat org.eclipse.jetty.server.handler.gzip.GzipHandler.handle(GzipHandler.java:396)\n\tat io.dropwizard.jetty.BiDiGzipHandler.handle(BiDiGzipHandler.java:68)\n\tat org.eclipse.jetty.server.handler.RequestLogHandler.handle(RequestLogHandler.java:56)\n\tat org.eclipse.jetty.server.handler.StatisticsHandler.handle(StatisticsHandler.java:169)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:134)\n\tat org.eclipse.jetty.server.Server.handle(Server.java:524)\n\tat org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:319)\n\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:253)\n\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:273)\n\tat org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:95)\n\tat org.eclipse.jetty.io.SelectChannelEndPoint$2.run(SelectChannelEndPoint.java:93)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.executeProduceConsume(ExecuteProduceConsume.java:303)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.produceConsume(ExecuteProduceConsume.java:148)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.run(ExecuteProduceConsume.java:136)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:671)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool$2.run(QueuedThreadPool.java:589)\n\tat java.lang.Thread.run(Thread.java:748) ''')
# print(tokens2)
#
# print(len(set(tokens1).intersection(set(tokens2))))
# print(len(set(tokens1).union(set(tokens2))))
#
# intersection = set(tokens1).intersection(set(tokens2))
# union = set(tokens1).union(set(tokens2))
# print (float(len(intersection)) / len(union))