// See IDiscordRpcService.aidl.
package com.discord.socialsdk.rpc;

interface IDiscordRpcCallback {
    /** One JSON frame from Discord: READY, a command's answer, or an ERROR. */
    void onFrame(String frame);
    void onClose(int code, String message);
}
