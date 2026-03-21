package com.atruedev.kmpble.connection

import com.atruedev.kmpble.error.BleError

public sealed interface State {

    /** Stable display name for logging. Avoids `::class.simpleName` which can return null on K/N. */
    public val displayName: String

    public sealed interface Connecting : State {
        public data object Transport : Connecting { override val displayName: String = "Transport" }
        public data object Authenticating : Connecting { override val displayName: String = "Authenticating" }
        public data object Discovering : Connecting { override val displayName: String = "Discovering" }
        public data object Configuring : Connecting { override val displayName: String = "Configuring" }
    }

    public sealed interface Connected : State {
        public data object Ready : Connected { override val displayName: String = "Ready" }
        public data object BondingChange : Connected { override val displayName: String = "BondingChange" }
        public data object ServiceChanged : Connected { override val displayName: String = "ServiceChanged" }
    }

    public sealed interface Disconnecting : State {
        public data object Requested : Disconnecting { override val displayName: String = "Requested" }
        public data object Error : Disconnecting { override val displayName: String = "Error" }
    }

    public sealed interface Disconnected : State {
        public data object ByRequest : Disconnected { override val displayName: String = "ByRequest" }
        public data object ByRemote : Disconnected { override val displayName: String = "ByRemote" }
        public data class ByError(val error: BleError) : Disconnected { override val displayName: String = "ByError" }
        public data object ByTimeout : Disconnected { override val displayName: String = "ByTimeout" }
        public data object BySystemEvent : Disconnected { override val displayName: String = "BySystemEvent" }
    }
}
