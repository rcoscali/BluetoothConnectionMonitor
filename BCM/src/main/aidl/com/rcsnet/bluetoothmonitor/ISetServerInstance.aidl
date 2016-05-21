// SetServerInstance.aidl
package com.rcsnet.bluetoothmonitor;

// Declare any non-default types here with import statements
import com.rcsnet.bluetoothmonitor.IClientServer;

interface ISetServerInstance {
    /**
     * Allows to set a server instance
     */
    void setServer(IClientServer server);
}
