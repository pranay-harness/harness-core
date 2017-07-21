import numpy as np

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


# x = [[1.,1,1,1], [1,1,1,0]]
#
# print(pairwise_jaccard_similarity(np.array(x)))
# print(jaccard_similarity(np.array(x), np.array([[1.,1,1,1], [3.,3,3,3], [0.,0.,3,3]])))