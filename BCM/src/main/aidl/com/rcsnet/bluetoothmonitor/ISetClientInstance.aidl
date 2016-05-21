// ISetClientInstance.aidl
package com.rcsnet.bluetoothmonitor;

import com.rcsnet.bluetoothmonitor.IClientServer;

// Declare any non-default types here with import statements

interface ISetClientInstance {
    /**
     * Allows to a the client instance
     */
    void setClient(IClientServer client);
}
