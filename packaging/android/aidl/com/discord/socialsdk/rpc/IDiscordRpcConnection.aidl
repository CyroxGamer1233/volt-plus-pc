// See IDiscordRpcService.aidl.
package com.discord.socialsdk.rpc;

interface IDiscordRpcConnection {
    /** One JSON frame: Discord's RPC commands, without the desktop's binary header. */
    void sendFrame(String frame);
    void disconnect();
}
