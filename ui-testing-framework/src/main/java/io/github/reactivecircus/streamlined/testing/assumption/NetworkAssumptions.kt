@file:Suppress("MagicNumber")

package io.github.reactivecircus.streamlined.testing.assumption

import retrofit2.mock.NetworkBehavior
import javax.inject.Inject

class NetworkAssumptions @Inject constructor(
    private val networkBehavior: NetworkBehavior
) {
    fun assumeNetworkConnected() {
        networkBehavior.setFailurePercent(0)
    }

    fun assumeNetworkDisconnected() {
        networkBehavior.setFailurePercent(100)
    }
}
