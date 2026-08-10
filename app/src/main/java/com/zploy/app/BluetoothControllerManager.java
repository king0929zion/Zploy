package com.zploy.app;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bluetooth-facing helper. Pairing/connection is delegated to Android's trusted system UI. */
public final class BluetoothControllerManager {
    public static final int REQUEST_CONNECT_PERMISSION = 4102;

    public static final class DeviceEntry {
        public final String name;
        public final String address;
        public final int bondState;

        DeviceEntry(String name, String address, int bondState) {
            this.name = name;
            this.address = address;
            this.bondState = bondState;
        }
    }

    private BluetoothControllerManager() {}

    public static boolean hasConnectPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasConnectPermission(activity)) {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_CONNECT_PERMISSION);
        }
    }

    public static boolean isBluetoothEnabled(Context context) {
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null) return false;
        if (!hasConnectPermission(context)) return false;
        try { return adapter.isEnabled(); } catch (SecurityException ignored) { return false; }
    }

    public static List<DeviceEntry> bonded(Context context) {
        List<DeviceEntry> out = new ArrayList<>();
        if (!hasConnectPermission(context)) return out;
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null) return out;
        try {
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                String name = device.getName();
                if (name == null || name.trim().isEmpty()) name = context.getString(R.string.unknown_device);
                out.add(new DeviceEntry(name, device.getAddress(), device.getBondState()));
            }
        } catch (SecurityException ignored) {}
        out.sort(Comparator.comparing(a -> a.name.toLowerCase()));
        return out;
    }

    public static void openBluetoothSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
