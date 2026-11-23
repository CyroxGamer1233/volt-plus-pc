package dev.horizon.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.discord.socialsdk.rpc.IDiscordRpcCallback;
import com.discord.socialsdk.rpc.IDiscordRpcConnection;
import com.discord.socialsdk.rpc.IDiscordRpcService;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Publishes the game's Rich Presence card on the player's Discord profile, from the phone.
 *
 * <p>The Discord Android app hosts a bound service ({@code
 * com.discord.socialsdk.rpc.IDiscordRpcService}, the transport its own Social SDK uses) that
 * turns JSON RPC frames into an activity card. This class binds that service, connects with the
 * Volt+ application id and hands over the card the game built; nothing else of Discord's is
 * involved, and no account token is ever seen by this app.
 *
 * <p>Everything here runs on the main looper: the game thread only posts, so a slow or missing
 * Discord never costs the game a frame. A Discord that is not running, is killed, or refuses the
 * connection is retried every {@link #RETRY_MILLIS}; the last card is sent again as soon as the
 * service says READY.
 */
final class DiscordPresence {
    private static final String TAG = "VoltDiscord";
    /** The bound service Discord's Social SDK publishes presence through. */
    private static final String ACTION = "com.discord.socialsdk.rpc.IDiscordRpcService";
    /** The Discord app, then the client mods that keep the same service. */
    private static final String[] CLIENTS = {"com.discord", "cocobo1.pupu.app"};
    private static final String RPC_VERSION = "1";
    /** Volt+ on Discord's developer portal (public: it names the card). */
    private static final long APPLICATION_ID = 1574331637087469588L;
    /**
     * The RPC pid: Discord only compares it with the nonce's answer, and the SDK's own frames
     * use 1 rather than the real process id.
     */
    private static final int HANDLER_PID = 1;
    private static final long RETRY_MILLIS = 15_000L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static Context app;
    private static IDiscordRpcService service;
    private static IDiscordRpcConnection connection;
    private static boolean bound;
    private static boolean ready;
    private static boolean wanted;
    /** The card to show: the last one the game built, resent on every READY. */
    private static String card;
    /**
     * Whether the installed Discord accepts the card's extra fields. A client that answers an
     * error to buttons or a party keeps the rest of the card: the picture, the links and the
     * two lines are what the card is for.
     */
    private static boolean extended = true;

    private DiscordPresence() {}

    /** Shows {@code json} (the activity the game built). Safe from the game thread. */
    public static void publish(Context context, String json) {
        if (context == null || json == null || json.isEmpty()) {
            return;
        }
        Context application = context.getApplicationContext();
        MAIN.post(
                () -> {
                    app = application == null ? context : application;
                    card = json;
                    wanted = true;
                    if (!bound) {
                        bind();
                    } else if (ready) {
                        send();
                    }
                });
    }

    /** Removes the card and lets Discord's service go. Safe from the game thread. */
    public static void clear() {
        MAIN.post(
                () -> {
                    card = null;
                    wanted = false;
                    if (ready) {
                        flush(clearFrame());
                    }
                    detach();
                });
    }

    private static void bind() {
        if (app == null || bound) {
            return;
        }
        Intent intent = serviceIntent();
        if (intent == null) {
            // The Discord app is not installed (or is being updated): try again later.
            retryLater();
            return;
        }
        try {
            bound = app.bindService(intent, CONNECTION, Context.BIND_AUTO_CREATE);
        } catch (Throwable error) {
            Log.i(TAG, "Discord's presence service refused binding: " + error);
            bound = false;
        }
        if (!bound) {
            retryLater();
        }
    }

    /** Discord's service as this device has it, or {@code null} when neither client is there. */
    private static Intent serviceIntent() {
        for (String client : CLIENTS) {
            Intent intent = new Intent(ACTION).setPackage(client);
            try {
                if (app.getPackageManager().resolveService(intent, 0) != null) {
                    return intent;
                }
            } catch (Throwable ignored) {
                // A package that cannot be resolved is one this device does not have.
            }
        }
        return null;
    }

    private static void connect() {
        if (service == null) {
            return;
        }
        IDiscordRpcConnection fresh;
        try {
            fresh = service.connect(APPLICATION_ID, RPC_VERSION, CALLBACK);
        } catch (Throwable error) {
            Log.i(TAG, "Discord refused the connection: " + error);
            fresh = null;
        }
        connection = fresh;
        ready = false;
        if (fresh == null) {
            detach();
            retryLater();
        }
    }

    /** Hands the current card to Discord; called once the service answered READY. */
    private static void send() {
        if (card == null || !ready) {
            return;
        }
        flush(cardFrame(extended ? card : withoutExtras(card)));
    }

    /** The card without the fields an older Discord client refuses. */
    private static String withoutExtras(String activity) {
        try {
            JSONObject card = new JSONObject(activity);
            card.remove("buttons");
            card.remove("party");
            return card.toString();
        } catch (Throwable error) {
            return null;
        }
    }

    /** One frame over the binder; a Discord that went away is re-bound, never retried blindly. */
    private static void flush(String frame) {
        IDiscordRpcConnection target = connection;
        if (target == null || frame == null) {
            return;
        }
        try {
            target.sendFrame(frame);
        } catch (Throwable error) {
            Log.i(TAG, "Discord presence dropped: " + error);
            detach();
            retryLater();
        }
    }

    /** {@code SET_ACTIVITY} with the game's card. */
    private static String cardFrame(String activity) {
        try {
            JSONObject args =
                    new JSONObject().put("pid", HANDLER_PID).put("activity", new JSONObject(activity));
            return new JSONObject()
                    .put("cmd", "SET_ACTIVITY")
                    .put("nonce", UUID.randomUUID().toString())
                    .put("args", args)
                    .toString();
        } catch (Throwable error) {
            // Malformed JSON is the game's bug: drop the card rather than send half of it.
            Log.i(TAG, "Discord presence card is not JSON: " + error);
            return null;
        }
    }

    /**
     * {@code SET_ACTIVITY} without an activity, which is how the Social SDK removes a card
     * ({@code args} holds only the pid).
     */
    private static String clearFrame() {
        try {
            return new JSONObject()
                    .put("cmd", "SET_ACTIVITY")
                    .put("nonce", UUID.randomUUID().toString())
                    .put("args", new JSONObject().put("pid", HANDLER_PID))
                    .toString();
        } catch (Throwable error) {
            return null;
        }
    }

    private static void detach() {
        ready = false;
        IDiscordRpcConnection closing = connection;
        connection = null;
        service = null;
        if (closing != null) {
            try {
                closing.disconnect();
            } catch (Throwable ignored) {
                // Already gone.
            }
        }
        if (bound) {
            try {
                if (app != null) {
                    app.unbindService(CONNECTION);
                }
            } catch (Throwable ignored) {
                // The binding was never established.
            }
            bound = false;
        }
    }

    private static void retryLater() {
        if (!wanted) {
            return;
        }
        MAIN.removeCallbacks(RETRY);
        MAIN.postDelayed(RETRY, RETRY_MILLIS);
    }

    private static final Runnable RETRY = () -> {
        if (wanted && app != null && !bound) {
            bind();
        }
    };

    private static final ServiceConnection CONNECTION =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder binder) {
                    service = IDiscordRpcService.Stub.asInterface(binder);
                    MAIN.post(DiscordPresence::connect);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    MAIN.post(
                            () -> {
                                ready = false;
                                connection = null;
                                service = null;
                                retryLater();
                            });
                }
            };

    private static final IDiscordRpcCallback.Stub CALLBACK =
            new IDiscordRpcCallback.Stub() {
                @Override
                public void onFrame(String frame) {
                    if (frame == null) {
                        return;
                    }
                    if (frame.contains("\"READY\"")) {
                        MAIN.post(
                                () -> {
                                    ready = true;
                                    if (wanted) {
                                        send();
                                    }
                                });
                    } else if (frame.contains("\"ERROR\"")) {
                        // A card Discord did not like; the log keeps the reason for the report.
                        Log.i(TAG, "Discord presence answered an error: " + frame);
                        MAIN.post(
                                () -> {
                                    if (extended && card != null) {
                                        extended = false;
                                        send();
                                    }
                                });
                    }
                }

                @Override
                public void onClose(int code, String message) {
                    MAIN.post(
                            () -> {
                                Log.i(TAG, "Discord closed the presence (" + code + ": " + message + ")");
                                ready = false;
                                connection = null;
                                retryLater();
                            });
                }
            };
}
