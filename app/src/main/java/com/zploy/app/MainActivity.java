package com.zploy.app;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity implements ControllerStore.Listener, ShizukuBridge.Listener {
    private FrameLayout pageHost;
    private LinearLayout nav;
    private int page = 0;
    private TextView deviceText;
    private TextView shizukuStatus;
    private TextView accessibilityStatus;
    private TextView startButton;
    private TextView rawText;
    private MappingStore mappingStore;
    private ProfileStore profileStore;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        mappingStore = new MappingStore(this);
        profileStore = new ProfileStore(this);
        ShizukuBridge.get().init(this);
        ShizukuBridge.get().addListener(this);
        ControllerStore.get().addListener(this);
        buildRoot();
        showPage(0);
    }

    @Override protected void onResume() {
        super.onResume();
        detectController();
        refreshHomeState();
    }

    @Override protected void onDestroy() {
        ControllerStore.get().removeListener(this);
        ShizukuBridge.get().removeListener(this);
        super.onDestroy();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (ControllerStore.isControllerSource(event.getSource())) ControllerStore.get().updateKey(event);
        return super.dispatchKeyEvent(event);
    }

    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (ControllerStore.isControllerSource(event.getSource())) ControllerStore.get().updateMotion(event);
        return super.dispatchGenericMotionEvent(event);
    }

    private void buildRoot() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        pageHost = new FrameLayout(this);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        hp.bottomMargin = Ui.dp(this, 86);
        root.addView(pageHost, hp);

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(Ui.dp(this, 7),Ui.dp(this,7),Ui.dp(this,7),Ui.dp(this,7));
        nav.setBackground(Ui.round(Ui.BLACK, 26, this));
        String[] names = {getString(R.string.home),getString(R.string.mapping),getString(R.string.test),getString(R.string.settings)};
        for (int i=0;i<names.length;i++) {
            TextView b = Ui.text(this,names[i],10,Color.rgb(130,130,130),true);
            b.setGravity(Gravity.CENTER);
            int index=i;
            b.setOnClickListener(v->showPage(index));
            nav.addView(b,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
        }
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,70));
        np.gravity=Gravity.BOTTOM; np.leftMargin=Ui.dp(this,14); np.rightMargin=Ui.dp(this,14); np.bottomMargin=Ui.dp(this,12);
        root.addView(nav,np);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            FrameLayout.LayoutParams navParams = (FrameLayout.LayoutParams) nav.getLayoutParams();
            navParams.bottomMargin = bars.bottom + Ui.dp(this, 12);
            nav.setLayoutParams(navParams);
            FrameLayout.LayoutParams hostParams = (FrameLayout.LayoutParams) pageHost.getLayoutParams();
            hostParams.bottomMargin = bars.bottom + Ui.dp(this, 86);
            pageHost.setLayoutParams(hostParams);
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();
    }

    private void showPage(int index) {
        page=index;
        pageHost.removeAllViews();
        View content;
        if (index==0) content=homePage();
        else if (index==1) content=mappingPage();
        else if (index==2) content=testPage();
        else content=settingsPage();
        pageHost.addView(content,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        for (int i=0;i<nav.getChildCount();i++) {
            TextView b=(TextView)nav.getChildAt(i);
            b.setTextColor(i==index?Color.WHITE:Color.rgb(125,125,125));
            b.setBackground(i==index?Ui.round(Color.rgb(42,42,42),19,this):null);
        }
        refreshHomeState();
    }

    private View pageShell(String title, LinearLayout body) {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout root=Ui.vertical(this); root.setPadding(Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,30));
        TextView t=Ui.text(this,title,26,Ui.BLACK,true); root.addView(t,new LinearLayout.LayoutParams(-1,Ui.dp(this,58)));
        root.addView(body,new LinearLayout.LayoutParams(-1,-2)); scroll.addView(root); return scroll;
    }

    private View homePage() {
        LinearLayout body=Ui.vertical(this);
        LinearLayout hero=Ui.vertical(this); hero.setPadding(Ui.dp(this,22),Ui.dp(this,20),Ui.dp(this,22),Ui.dp(this,22)); hero.setBackground(Ui.round(Ui.BLACK,32,this));
        TextView label=Ui.text(this,getString(R.string.controller),11,Color.rgb(150,150,150),false); hero.addView(label);
        deviceText=Ui.text(this,"Gamepad",26,Color.WHITE,true); LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,Ui.dp(this,55)); hero.addView(deviceText,dp);
        startButton=Ui.button(this,Prefs.mappingEnabled(this)?getString(R.string.stop_mapping):getString(R.string.start_mapping),false);
        startButton.setOnClickListener(v->toggleMapping()); hero.addView(startButton,new LinearLayout.LayoutParams(-1,Ui.dp(this,54)));
        body.addView(hero,new LinearLayout.LayoutParams(-1,-2));

        body.addView(sectionTitle(getString(R.string.service_ready)));
        body.addView(statusCard(true));
        body.addView(statusCard(false));
        return pageShell("Zploy",body);
    }

    private TextView sectionTitle(String s) {
        TextView t=Ui.text(this,s,14,Ui.BLACK,true); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,Ui.dp(this,48)); p.topMargin=Ui.dp(this,10); t.setLayoutParams(p); return t;
    }

    private View statusCard(boolean shizuku) {
        LinearLayout card=new LinearLayout(this); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(Ui.dp(this,17),Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14)); card.setBackground(Ui.round(Ui.WHITE,25,this));
        LinearLayout copy=Ui.vertical(this);
        TextView title=Ui.text(this,shizuku?getString(R.string.shizuku):getString(R.string.accessibility),14,Ui.BLACK,true); copy.addView(title);
        TextView status=Ui.text(this,"",11,Ui.GRAY,false); copy.addView(status);
        if(shizuku) shizukuStatus=status; else accessibilityStatus=status;
        card.addView(copy,new LinearLayout.LayoutParams(0,Ui.dp(this,55),1));
        TextView action=Ui.text(this,shizuku?getString(R.string.authorize):getString(R.string.open_settings),11,Ui.BLACK,true); action.setGravity(Gravity.CENTER); action.setBackground(Ui.round(Ui.LIGHT,14,this));
        action.setOnClickListener(v->{ if(shizuku) onShizukuAction(); else startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); });
        card.addView(action,new LinearLayout.LayoutParams(Ui.dp(this,86),Ui.dp(this,38)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=Ui.dp(this,10); card.setLayoutParams(p); return card;
    }

    private View mappingPage() {
        LinearLayout body=Ui.vertical(this);
        body.addView(settingRow(getString(R.string.profile), profileStore.activeName(), v -> profileDialog()));
        TextView open=Ui.button(this,getString(R.string.open_overlay_editor),true); open.setOnClickListener(v->{
            ZployAccessibilityService s=ZployAccessibilityService.getInstance();
            if(s==null){toast(getString(R.string.need_accessibility)); return;}
            if(!Prefs.mappingEnabled(this)) s.applyMappingState(true);
            s.openEditor();
            toast(getString(R.string.press_button_to_add));
        });
        body.addView(open,new LinearLayout.LayoutParams(-1,Ui.dp(this,54)));
        TextView hint=Ui.text(this,getString(R.string.overlay_hint),12,Ui.GRAY,false); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.topMargin=Ui.dp(this,12); hp.bottomMargin=Ui.dp(this,18); body.addView(hint,hp);
        for(MappingItem item:mappingStore.load()) body.addView(mappingRow(item));
        return pageShell(getString(R.string.mapping),body);
    }

    private View mappingRow(MappingItem item) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(Ui.dp(this,15),Ui.dp(this,12),Ui.dp(this,15),Ui.dp(this,12)); row.setBackground(Ui.round(Ui.WHITE,23,this));
        TextView key=Ui.text(this,item.label,13,Ui.BLACK,true); key.setGravity(Gravity.CENTER); key.setBackground(Ui.round(Ui.LIGHT,14,this)); row.addView(key,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,44)));
        LinearLayout copy=Ui.vertical(this); TextView name=Ui.text(this,typeName(item.type),13,Ui.BLACK,true); TextView meta=Ui.text(this,String.format(Locale.US,"x %.2f · y %.2f",item.x,item.y),10,Ui.GRAY,false); copy.addView(name); copy.addView(meta); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1); cp.leftMargin=Ui.dp(this,13); row.addView(copy,cp);
        TextView edit=Ui.text(this,"›",24,Ui.GRAY,false); edit.setGravity(Gravity.CENTER); row.addView(edit,new LinearLayout.LayoutParams(Ui.dp(this,34),Ui.dp(this,44)));
        row.setOnClickListener(v->showMappingDialog(item));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=Ui.dp(this,9); row.setLayoutParams(p); return row;
    }

    private void showMappingDialog(MappingItem item) {
        boolean analog = item.keyCode == MappingItem.KEY_LEFT_STICK || item.keyCode == MappingItem.KEY_RIGHT_STICK;
        String[] names = analog
                ? new String[]{getString(R.string.joystick), getString(R.string.camera)}
                : new String[]{getString(R.string.tap), getString(R.string.hold)};
        new AlertDialog.Builder(this).setTitle(item.label).setItems(names,(d,which)->{
            item.type = analog
                    ? (which == 0 ? MappingType.JOYSTICK : MappingType.CAMERA)
                    : (which == 0 ? MappingType.TAP : MappingType.HOLD);
            List<MappingItem> list=mappingStore.load();
            for(int i=0;i<list.size();i++) if(list.get(i).id.equals(item.id)){list.set(i,item);break;}
            mappingStore.save(list);
            ZployAccessibilityService s=ZployAccessibilityService.getInstance();
            if(s!=null)s.refreshOverlay();
            showPage(1);
        }).show();
    }

    private View testPage() {
        LinearLayout body=Ui.vertical(this);
        TextView hint=Ui.text(this,getString(R.string.controller_test_hint),12,Ui.GRAY,false); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.bottomMargin=Ui.dp(this,12); body.addView(hint,hp);
        ControllerView view=new ControllerView(this); view.setBackground(Ui.round(Ui.WHITE,28,this)); body.addView(view,new LinearLayout.LayoutParams(-1,Ui.dp(this,290)));
        body.addView(sectionTitle(getString(R.string.raw_input)));
        rawText=Ui.text(this,rawInputText(ControllerStore.get().snapshot()),12,Ui.BLACK,false); rawText.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16)); rawText.setBackground(Ui.round(Ui.WHITE,24,this)); body.addView(rawText,new LinearLayout.LayoutParams(-1,-2));
        return pageShell(getString(R.string.test),body);
    }

    private View settingsPage() {
        LinearLayout body=Ui.vertical(this);
        body.addView(settingRow(getString(R.string.profile), profileStore.activeName(), v -> profileManageDialog()));
        body.addView(settingRow(getString(R.string.language),currentLanguage(),v->languageDialog()));
        body.addView(settingRow(getString(R.string.backend),backendName(),v->backendDialog()));

        LinearLayout alpha=Ui.vertical(this); alpha.setPadding(Ui.dp(this,17),Ui.dp(this,15),Ui.dp(this,17),Ui.dp(this,15)); alpha.setBackground(Ui.round(Ui.WHITE,25,this));
        TextView at=Ui.text(this,getString(R.string.overlay_alpha),13,Ui.BLACK,true); alpha.addView(at);
        SeekBar bar=new SeekBar(this); bar.setMax(55); bar.setProgress(Math.round((Prefs.overlayAlpha(this)-.10f)*100)); bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){Prefs.setOverlayAlpha(MainActivity.this,.10f+p/100f); ZployAccessibilityService z=ZployAccessibilityService.getInstance(); if(z!=null)z.refreshOverlay();} public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}}); alpha.addView(bar,new LinearLayout.LayoutParams(-1,Ui.dp(this,44)));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2); ap.bottomMargin=Ui.dp(this,10); body.addView(alpha,ap);
        body.addView(settingRow(getString(R.string.about),getString(R.string.version_label),v->{}));
        return pageShell(getString(R.string.settings),body);
    }

    private View settingRow(String title,String sub,View.OnClickListener click) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(Ui.dp(this,17),Ui.dp(this,14),Ui.dp(this,17),Ui.dp(this,14)); row.setBackground(Ui.round(Ui.WHITE,25,this));
        LinearLayout copy=Ui.vertical(this); copy.addView(Ui.text(this,title,13,Ui.BLACK,true)); copy.addView(Ui.text(this,sub,10,Ui.GRAY,false)); row.addView(copy,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1)); row.addView(Ui.text(this,"›",24,Ui.GRAY,false),new LinearLayout.LayoutParams(Ui.dp(this,28),Ui.dp(this,50))); row.setOnClickListener(click);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=Ui.dp(this,10); row.setLayoutParams(p); return row;
    }

    private void profileDialog() {
        List<ProfileStore.Profile> profiles = profileStore.load();
        String[] names = new String[profiles.size() + 1];
        for (int i = 0; i < profiles.size(); i++) names[i] = profiles.get(i).name;
        names[profiles.size()] = "+ " + getString(R.string.new_profile);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.profile))
                .setItems(names, (d, which) -> {
                    if (which < profiles.size()) {
                        profileStore.setActive(profiles.get(which).id);
                        ZployAccessibilityService z = ZployAccessibilityService.getInstance();
                        if (z != null) z.refreshOverlay();
                        showPage(1);
                    } else {
                        createProfileDialog();
                    }
                })
                .setPositiveButton(getString(R.string.manage), (d, w) -> profileManageDialog())
                .show();
    }

    private void profileManageDialog() {
        String[] actions = {getString(R.string.new_profile), getString(R.string.rename), getString(R.string.delete)};
        new AlertDialog.Builder(this)
                .setTitle(profileStore.activeName())
                .setItems(actions, (d, which) -> {
                    if (which == 0) createProfileDialog();
                    else if (which == 1) renameProfileDialog();
                    else deleteActiveProfile();
                }).show();
    }

    private void createProfileDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(getString(R.string.profile_name));
        int pad = Ui.dp(this, 20);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(pad, 0, pad, 0);
        wrap.addView(input, new FrameLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.new_profile))
                .setView(wrap)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    profileStore.create(input.getText().toString());
                    ZployAccessibilityService z = ZployAccessibilityService.getInstance();
                    if (z != null) z.refreshOverlay();
                    showPage(page == 3 ? 3 : 1);
                })
                .setNegativeButton(getString(R.string.close), null)
                .show();
    }

    private void renameProfileDialog() {
        String id = profileStore.activeId();
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(profileStore.activeName());
        input.setSelection(input.getText().length());
        int pad = Ui.dp(this, 20);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(pad, 0, pad, 0);
        wrap.addView(input, new FrameLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.rename))
                .setView(wrap)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    profileStore.rename(id, input.getText().toString());
                    showPage(page);
                })
                .setNegativeButton(getString(R.string.close), null)
                .show();
    }

    private void deleteActiveProfile() {
        String id = profileStore.activeId();
        String name = profileStore.activeName();
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete))
                .setMessage(name)
                .setPositiveButton(getString(R.string.delete), (d, w) -> {
                    if (!profileStore.delete(id)) {
                        toast(getString(R.string.cannot_delete_last_profile));
                        return;
                    }
                    mappingStore.clearProfile(id);
                    ZployAccessibilityService z = ZployAccessibilityService.getInstance();
                    if (z != null) z.refreshOverlay();
                    showPage(page);
                })
                .setNegativeButton(getString(R.string.close), null)
                .show();
    }

    private void languageDialog(){String[] a={getString(R.string.follow_system),getString(R.string.chinese),getString(R.string.english)}; new AlertDialog.Builder(this).setTitle(getString(R.string.language)).setItems(a,(d,w)->{Prefs.setLanguage(this,w==1?"zh-CN":w==2?"en":""); recreate();}).show();}
    private void backendDialog(){String[] a={getString(R.string.backend_auto),getString(R.string.backend_shizuku),getString(R.string.backend_accessibility)}; new AlertDialog.Builder(this).setTitle(getString(R.string.backend)).setItems(a,(d,w)->{Prefs.setBackend(this,w==1?Prefs.BACKEND_SHIZUKU:w==2?Prefs.BACKEND_ACCESSIBILITY:Prefs.BACKEND_AUTO); showPage(3);}).show();}

    private String backendName(){String b=Prefs.backend(this); if(Prefs.BACKEND_SHIZUKU.equals(b))return getString(R.string.backend_shizuku); if(Prefs.BACKEND_ACCESSIBILITY.equals(b))return getString(R.string.backend_accessibility); return getString(R.string.backend_auto);}
    private String currentLanguage(){String tag=getResources().getConfiguration().getLocales().get(0).toLanguageTag(); return tag.startsWith("zh")?getString(R.string.chinese):getString(R.string.english);}
    private String typeName(MappingType t){switch(t){case HOLD:return getString(R.string.hold);case JOYSTICK:return getString(R.string.joystick);case CAMERA:return getString(R.string.camera);default:return getString(R.string.tap);}}

    private void toggleMapping() {
        boolean next=!Prefs.mappingEnabled(this);
        ZployAccessibilityService s=ZployAccessibilityService.getInstance();
        if(s==null){toast(getString(R.string.need_accessibility)); return;}
        String backend=Prefs.backend(this);
        if(next && Prefs.BACKEND_SHIZUKU.equals(backend) && !ShizukuBridge.get().isReady()){toast(getString(R.string.need_shizuku));return;}
        s.applyMappingState(next); refreshHomeState(); toast(next?getString(R.string.mapping_started):getString(R.string.mapping_stopped));
    }

    private void onShizukuAction() {
        if(!ShizukuBridge.get().isBinderAlive()) {
            Intent launch=getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
            if(launch!=null)startActivity(launch); else toast(getString(R.string.shizuku_missing));
        } else ShizukuBridge.get().requestPermission();
    }

    private void refreshHomeState() {
        if(deviceText!=null){ControllerState s=ControllerStore.get().snapshot(); deviceText.setText(TextUtils.isEmpty(s.deviceName)?getString(R.string.not_connected):s.deviceName);}
        if(shizukuStatus!=null){if(ShizukuBridge.get().isReady())shizukuStatus.setText(getString(R.string.shizuku_ready)); else if(ShizukuBridge.get().isBinderAlive())shizukuStatus.setText(getString(R.string.shizuku_waiting)); else shizukuStatus.setText(getString(R.string.shizuku_missing));}
        if(accessibilityStatus!=null)accessibilityStatus.setText(isAccessibilityEnabled()?getString(R.string.accessibility_ready):getString(R.string.accessibility_missing));
        if(startButton!=null)startButton.setText(Prefs.mappingEnabled(this)?getString(R.string.stop_mapping):getString(R.string.start_mapping));
    }

    private boolean isAccessibilityEnabled() {
        String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(enabled==null)return false; ComponentName cn=new ComponentName(this,ZployAccessibilityService.class); return enabled.toLowerCase(Locale.ROOT).contains(cn.flattenToString().toLowerCase(Locale.ROOT));
    }

    private void detectController() {
        for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id); if(d!=null && ControllerStore.isControllerSource(d.getSources())){ControllerStore.get().setDevice(d); return;}}
    }

    private String rawInputText(ControllerState s) {
        StringBuilder pressed = new StringBuilder();
        for (int key : s.pressedKeys) {
            if (pressed.length() > 0) pressed.append("  ");
            pressed.append(MappingStore.labelForKey(key)).append('(').append(key).append(')');
        }
        int[] synthesized = {
                KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT
        };
        for (int key : synthesized) {
            if (s.pressed(key) && !s.pressedKeys.contains(key)) {
                if (pressed.length() > 0) pressed.append("  ");
                pressed.append(MappingStore.labelForKey(key)).append("(axis)");
            }
        }
        if (pressed.length() == 0) pressed.append('—');
        return String.format(Locale.US,
                "Device: %s\nID: %d\nLX  %+.3f    LY  %+.3f\nRX  %+.3f    RY  %+.3f\nLT  %.3f     RT  %.3f\nHAT %+.1f / %+.1f\nPressed: %s\nLast key: %s (%d)",
                TextUtils.isEmpty(s.deviceName)?"—":s.deviceName,s.deviceId,s.lx,s.ly,s.rx,s.ry,s.lt,s.rt,s.hatX,s.hatY,
                pressed, KeyEvent.keyCodeToString(s.lastKeyCode), s.lastKeyCode);
    }

    @Override public void onControllerState(ControllerState state) { runOnUiThread(()->{if(rawText!=null)rawText.setText(rawInputText(state)); if(deviceText!=null)deviceText.setText(TextUtils.isEmpty(state.deviceName)?getString(R.string.not_connected):state.deviceName);}); }
    @Override public void onShizukuStateChanged() { runOnUiThread(this::refreshHomeState); }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
