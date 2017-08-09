import numpy as np
from Tokenizer import Tokenizer
from TFIDFVectorizer import TFIDFVectorizer
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
    :param Y: 1 x n sparse matrix
    :return: jaccard similarity score between X and Y
    """

    X = X.astype(bool).astype(float)
    Y = Y.astype(bool).astype(float)

    intrsct = X.dot(Y.T)
    unions = np.sum(X, axis=1) + np.sum(Y, axis=1) - intrsct
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

#TODO pass tokenizer as argument
def jaccard_difference(control_text, test_text):
    test_tokens = Tokenizer.default_tokenizer(test_text)
    control_tokens = Tokenizer.default_tokenizer(control_text)
    return list((set(control_tokens) - set(test_tokens)).union(set(test_tokens) - set(control_tokens)))





# x = [[1.,1,1,1], [1,1,1,0]]
#
# print(pairwise_jaccard_similarity(np.array(x)))
# print(jaccard_similarity(np.array(x), np.array([[1.,1,1,1], [3.,3,3,3], [0.,0.,3,3]])))


# vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, 1, 1.0)
# X = vectorizer.fit_transform([''' 2017-07-21 22:29:22,832 \u001b[32m[dw-142 - POST /api/settings?&accountId=kmpySmUISimoRrJL6NL73w]\u001b[0;39m \u001b[1;31mERROR\u001b[0;39m \u001b[36msoftware.wings.exception.WingsExceptionMapper\u001b[0;39m - Exception occurred: INVALID_ARTIFACT_SERVER \nsoftware.wings.exception.WingsException: INVALID_ARTIFACT_SERVER\n\tat software.wings.service.impl.BambooBuildServiceImpl.validateArtifactServer(BambooBuildServiceImpl.java:79)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:498)\n\tat org.joor.Reflect.on(Reflect.java:677)\n\tat org.joor.Reflect.call(Reflect.java:379)\n\tat software.wings.delegatetasks.ServiceImplDelegateTask.run(ServiceImplDelegateTask.java:46)\n\tat software.wings.delegatetasks.ServiceImplDelegateTask.run(ServiceImplDelegateTask.java:19)\n\tat software.wings.delegatetasks.AbstractDelegateRunnableTask.run(AbstractDelegateRunnableTask.java:32)\n\tat java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)\n\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\n\tat java.lang.Thread.run(Thread.java:748) ''',
#                           ''' 2017-07-21 22:29:25,241 \u001b[32m[dw-9346 - POST /api/settings?&accountId=kmpySmUISimoRrJL6NL73w]\u001b[0;39m \u001b[1;31mERROR\u001b[0;39m \u001b[36msoftware.wings.exception.WingsExceptionMapper\u001b[0;39m - Exception occurred: INVALID_ARTIFACT_SERVER \nsoftware.wings.exception.WingsException: INVALID_ARTIFACT_SERVER\n\tat software.wings.service.impl.BambooBuildServiceImpl.validateArtifactServer(BambooBuildServiceImpl.java:79)\n\tat sun.reflect.GeneratedMethodAccessor38.invoke(Unknown Source)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:498)\n\tat org.joor.Reflect.on(Reflect.java:677)\n\tat org.joor.Reflect.call(Reflect.java:379)\n\tat software.wings.delegatetasks.ServiceImplDelegateTask.run(ServiceImplDelegateTask.java:46)\n\tat software.wings.delegatetasks.ServiceImplDelegateTask.run(ServiceImplDelegateTask.java:19)\n\tat software.wings.delegatetasks.AbstractDelegateRunnableTask.run(AbstractDelegateRunnableTask.java:32)\n\tat java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)\n\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\n\tat java.lang.Thread.run(Thread.java:748) ''',
#                               ''' 2017-07-21 22:29:28,870 \u001b[32m[dw-150 - POST /api/settings?&accountId=kmpySmUISimoRrJL6NL73w]\u001b[0;39m \u001b[1;31mERROR\u001b[0;39m \u001b[36msoftware.wings.exception.WingsExceptionMapper\u001b[0;39m - Exception occurred: INVALID_ARTIFACT_SERVER \nsoftware.wings.exception.WingsException: INVALID_ARTIFACT_SERVER\n\tat software.wings.helpers.ext.bamboo.BambooServiceImpl.getHttpRequestExecutionResponse(BambooServiceImpl.java:126)\n\tat software.wings.helpers.ext.bamboo.BambooServiceImpl.getPlanKeys(BambooServiceImpl.java:104)\n\tat software.wings.helpers.ext.bamboo.BambooServiceImpl.isRunning(BambooServiceImpl.java:200)\n\tat software.wings.service.impl.BambooBuildServiceImpl.validateArtifactServer(BambooBuildServiceImpl.java:82)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:498)\n\tat org.joor.Reflect.on(Reflect.java:677)\n\tat org.joor.Reflect.call(Reflect.java:379)\n\tat software.wings.delegatetasks.ServiceImplDelegateTask.run(ServiceImplDelegateTask.java:46)\n\tat software.wings.delegatetasks.ServiceImplDelegateTask.run(ServiceImplDelegateTask.java:19)\n\tat software.wings.delegatetasks.AbstractDelegateRunnableTask.run(AbstractDelegateRunnableTask.java:32)\n\tat java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)\n\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\n\tat java.lang.Thread.run(Thread.java:748) '''])
# print(jaccard_similarity(X[0], X[1]))
# print(pairwise_jaccard_similarity(X))
#
# tokens1 = Tokenizer.default_tokenizer(
#     ''' 2017-07-21 22:29:22,832 \u001b[32m[dw-142 - POST /api/settings?&accountId=kmpySmUISimoRrJL6NL73w]\u001b[0;39m \u001b[1;31mERROR\u001b[0;39m \u001b[36msoftware.wings.exception.WingsExceptionMapper\u001b[0;39m - Exception occurred: INVALID_ARTIFACT_SERVER \nsoftware.wings.exception.WingsException: INVALID_ARTIFACT_SERVER\n\tat software.wings.service.impl.BambooBuildServiceImpl.validateArtifactServer(BambooBuildServiceImpl.java:79)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:498)\n\tat org.joor.Reflect.on(Reflect.java:677)\n\tat org.joor.Reflect.call(Reflect.java:379)\n\tat software.wings.delegatetasks.ServiceImplDelegateTask.run(ServiceImplDelegateTask.java:46)\n\tat software.wings.delegatetasks.ServiceImplDelegateTask.run(ServiceImplDelegateTask.java:19)\n\tat software.wings.delegatetasks.AbstractDelegateRunnableTask.run(AbstractDelegateRunnableTask.java:32)\n\tat java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)\n\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\n\tat java.lang.Thread.run(Thread.java:748) ''')
# print(tokens1)
# tokens2 = Tokenizer.default_tokenizer(
#     ''' 2017-07-21 22:29:25,241 \u001b[32m[dw-9346 - POST /api/settings?&accountId=kmpySmUISimoRrJL6NL73w]\u001b[0;39m \u001b[1;31mERROR\u001b[0;39m \u001b[36msoftware.wings.exception.WingsExceptionMapper\u001b[0;39m - Exception occurred: INVALID_ARTIFACT_SERVER \nsoftware.wings.exception.WingsException: INVALID_ARTIFACT_SERVER\n\tat software.wings.service.impl.BambooBuildServiceImpl.validateArtifactServer(BambooBuildServiceImpl.java:79)\n\tat sun.reflect.GeneratedMethodAccessor38.invoke(Unknown Source)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:498)\n\tat org.joor.Reflect.on(Reflect.java:677)\n\tat org.joor.Reflect.call(Reflect.java:379)\n\tat software.wings.delegatetasks.ServiceImplDelegateTask.run(ServiceImplDelegateTask.java:46)\n\tat software.wings.delegatetasks.ServiceImplDelegateTask.run(ServiceImplDelegateTask.java:19)\n\tat software.wings.delegatetasks.AbstractDelegateRunnableTask.run(AbstractDelegateRunnableTask.java:32)\n\tat java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)\n\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\n\tat java.lang.Thread.run(Thread.java:748) ''')
# print(tokens2)
#
# a = (float(len(set(tokens1).intersection(set(tokens2)))) / len((set(tokens1))))
# b = (float(len(set(tokens2).intersection(set(tokens1)))) / len((set(tokens2))))
#
# #print(((a + b) / 2.0) * (float(min(len(set(tokens1)), len(set(tokens2)))) / max(len(set(tokens1)), len(set(tokens2)))))**2
# print(min(a , b) * (float(min(len(set(tokens1)), len(set(tokens2)))) / max(len(set(tokens1)), len(set(tokens2)))))
#
# print((set(tokens1) - set(tokens2)).union(set(tokens2) - set(tokens1)))
# print(set(tokens1).intersection(set(tokens2)))
# tokens1_set = set(tokens1) #- set(['sun.reflect.generatedmethodaccessor38.invoke', 'sun.reflect.nativemethodaccessorimpl.invoke', 'sun.reflect.nativemethodaccessorimpl.invoke0', 'source', 'unknown', 'method', 'native'])
# tokens2_set = set(tokens2) #- set(['sun.reflect.generatedmethodaccessor38.invoke', 'sun.reflect.nativemethodaccessorimpl.invoke', 'sun.reflect.nativemethodaccessorimpl.invoke0', 'source', 'unknown', 'method', 'native'])
# print((tokens1_set - tokens2_set).union(tokens2_set - tokens1_set))
# print(len(tokens1_set.intersection(tokens2_set)))
# print(len(tokens1_set.union(tokens2_set)))
#
# intersection = tokens1_set.intersection(tokens2_set)
#
# union = tokens1_set.union(tokens2_set)
# print (float(len(intersection)) / len(union))