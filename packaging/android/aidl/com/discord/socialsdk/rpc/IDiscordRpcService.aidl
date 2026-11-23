// The bound service the Discord Android app publishes Rich Presence through (the transport its
// own Social SDK uses). Only the shapes the client needs are declared here; see
// dev.horizon.android.DiscordPresence for how they are used.
package com.discord.socialsdk.rpc;

import com.discord.socialsdk.rpc.IDiscordRpcCallback;
import com.discord.socialsdk.rpc.IDiscordRpcConnection;

interface IDiscordRpcService {
    /**
     * @param applicationId the Discord application whose card the game shows
     * @param version the protocol version, always "1"
     * @param callback receives Discord's frames, READY first
     */
    IDiscordRpcConnection connect(long applicationId, String version, IDiscordRpcCallback callback);
}
