package com.zploy.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;

import java.util.concurrent.CopyOnWriteArrayList;

import rikka.shizuku.Shizuku;

public final class ShizukuBridge {
    public interface Listener { void onShizukuStateChanged(); }

    private static final ShizukuBridge INSTANCE = new ShizukuBridge();
    public static ShizukuBridge get() { return INSTANCE; }

    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private Context appContext;
    private volatile IInputInjector injector;
    private volatile boolean binding;
    private volatile boolean initialized;

    private final Shizuku.OnBinderReceivedListener binderReceived = () -> {
        notifyListeners();
        if (hasPermission()) bindInputService();
    };

    private final Shizuku.OnBinderDeadListener binderDead = () -> {
        injector = null;
        binding = false;
        notifyListeners();
    };

    private final Shizuku.OnRequestPermissionResultListener permissionResult = (requestCode, grantResult) -> {
        notifyListeners();
        if (grantResult == PackageManager.PERMISSION_GRANTED) bindInputService();
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            injector = IInputInjector.Stub.asInterface(service);
            binding = false;
            notifyListeners();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            injector = null;
            binding = false;
            notifyListeners();
        }
    };

    private ShizukuBridge() {}

    public synchronized void init(Context context) {
        if (initialized) return;
        initialized = true;
        appContext = context.getApplicationContext();
        Shizuku.addBinderReceivedListenerSticky(binderReceived);
        Shizuku.addBinderDeadListener(binderDead);
        Shizuku.addRequestPermissionResultListener(permissionResult);
        if (isBinderAlive() && hasPermission()) bindInputService();
    }

    public void addListener(Listener l) { listeners.addIfAbsent(l); }
    public void removeListener(Listener l) { listeners.remove(l); }

    private void notifyListeners() {
        for (Listener l : listeners) l.onShizukuStateChanged();
    }

    public boolean isBinderAlive() {
        try { return Shizuku.pingBinder(); } catch (Throwable ignored) { return false; }
    }

    public boolean hasPermission() {
        if (!isBinderAlive()) return false;
        try { return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
        catch (Throwable ignored) { return false; }
    }

    public boolean isReady() {
        IInputInjector i = injector;
        if (!hasPermission() || i == null) return false;
        try { return i.ping(); } catch (Throwable ignored) { return false; }
    }

    public int shizukuUid() {
        try { return Shizuku.getUid(); } catch (Throwable ignored) { return -1; }
    }

    public void requestPermission() {
        if (!isBinderAlive()) return;
        try {
            if (!hasPermission()) Shizuku.requestPermission(1001);
            else bindInputService();
        } catch (Throwable ignored) {
        }
    }

    public synchronized void bindInputService() {
        if (appContext == null || binding || injector != null || !hasPermission()) return;
        binding = true;
        try {
            ComponentName component = new ComponentName(appContext.getPackageName(), InputInjectorService.class.getName());
            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(component)
                    .processNameSuffix("zploy_input")
                    .tag("zploy_input")
                    .debuggable(BuildConfig.DEBUG)
                    .version(1)
                    .daemon(false);
            Shizuku.bindUserService(args, connection);
        } catch (Throwable e) {
            binding = false;
            notifyListeners();
        }
    }

    public boolean injectMotion(int action, long downTime, long eventTime,
                                int[] ids, float[] xs, float[] ys) {
        IInputInjector i = injector;
        if (i == null) return false;
        try {
            return i.injectMotion(action, downTime, eventTime, ids, xs, ys);
        } catch (Throwable e) {
            injector = null;
            notifyListeners();
            return false;
        }
    }
}
