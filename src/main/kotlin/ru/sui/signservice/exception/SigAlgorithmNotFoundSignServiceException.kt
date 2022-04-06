package ru.sui.signservice.exception

import java.security.PrivateKey

class SigAlgorithmNotFoundSignServiceException(key: PrivateKey) : NotFoundSignServiceException("SigAlgorithm for key algorithm '${key.algorithm}' not found")