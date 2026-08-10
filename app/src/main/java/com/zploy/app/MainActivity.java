package com.zploy.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity implements ControllerStore.Listener, ShizukuBridge.Listener {
    private FrameLayout pageHost;
    private LinearLayout nav;
    private int page;
    private TextView controllerStatus;
    private TextView mappingButton;
    private TextView rawInput;
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
        BluetoothControllerManager.requestPermission(this);
        buildRoot();
        showPage(0);
    }

    @Override protected void onResume() {
        super.onResume();
        detectController();
        showPage(page);
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
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-1, -1);
        hp.bottomMargin = Ui.dp(this, 88);
        root.addView(pageHost, hp);

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(Ui.dp(this, 7), Ui.dp(this, 7), Ui.dp(this, 7), Ui.dp(this, 7));
        nav.setBackground(Ui.round(Ui.BLACK, 26, this));
        String[] labels = {getString(R.string.home), getString(R.string.games), getString(R.string.controller), getString(R.string.settings)};
        for (int i = 0; i < labels.length; i++) {
            TextView b = Ui.text(this, labels[i], 10, Color.rgb(130,130,130), true);
            b.setGravity(Gravity.CENTER);
            int index = i;
            b.setOnClickListener(v -> showPage(index));
            nav.addView(b, new LinearLayout.LayoutParams(0, -1, 1));
        }
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, Ui.dp(this, 70));
        np.gravity = Gravity.BOTTOM; np.leftMargin = Ui.dp(this, 14); np.rightMargin = Ui.dp(this, 14); np.bottomMargin = Ui.dp(this, 12);
        root.addView(nav, np);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            FrameLayout.LayoutParams n = (FrameLayout.LayoutParams) nav.getLayoutParams();
            n.bottomMargin = bars.bottom + Ui.dp(this, 12); nav.setLayoutParams(n);
            FrameLayout.LayoutParams h = (FrameLayout.LayoutParams) pageHost.getLayoutParams();
            h.bottomMargin = bars.bottom + Ui.dp(this, 88); pageHost.setLayoutParams(h);
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();
    }

    private void showPage(int index) {
        page = index;
        pageHost.removeAllViews();
        View content = index == 0 ? homePage() : index == 1 ? gamesPage() : index == 2 ? controllerPage() : settingsPage();
        pageHost.addView(content, new FrameLayout.LayoutParams(-1, -1));
        for (int i = 0; i < nav.getChildCount(); i++) {
            TextView b = (TextView) nav.getChildAt(i);
            b.setTextColor(i == index ? Color.WHITE : Color.rgb(125,125,125));
            b.setBackground(i == index ? Ui.round(Color.rgb(42,42,42), 19, this) : null);
        }
    }

    private View pageShell(String title, LinearLayout body) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = Ui.vertical(this);
        root.setPadding(Ui.dp(this,18), Ui.dp(this,18), Ui.dp(this,18), Ui.dp(this,32));
        root.addView(Ui.text(this, title, 26, Ui.BLACK, true), new LinearLayout.LayoutParams(-1, Ui.dp(this,58)));
        root.addView(body, new LinearLayout.LayoutParams(-1,-2));
        scroll.addView(root);
        return scroll;
    }

    private View homePage() {
        LinearLayout body = Ui.vertical(this);
        ProfileStore.Profile active = profileStore.active();
        LinearLayout hero = Ui.vertical(this);
        hero.setPadding(Ui.dp(this,22),Ui.dp(this,20),Ui.dp(this,22),Ui.dp(this,22));
        hero.setBackground(Ui.round(Ui.BLACK,32,this));
        hero.addView(Ui.text(this, getString(R.string.current_game), 11, Color.rgb(155,155,155), false));
        String game = active.isBound() ? active.appLabel : getString(R.string.no_game_selected);
        hero.addView(Ui.text(this, game, 27, Color.WHITE, true), new LinearLayout.LayoutParams(-1, Ui.dp(this,55)));
        hero.addView(Ui.text(this, active.isBound() ? active.packageName : getString(R.string.add_game_first), 10, Color.rgb(155,155,155), false), new LinearLayout.LayoutParams(-1, Ui.dp(this,28)));
        mappingButton = Ui.button(this, Prefs.mappingEnabled(this) ? getString(R.string.stop_mapping) : getString(R.string.start_mapping), false);
        mappingButton.setOnClickListener(v -> toggleMapping());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, Ui.dp(this,54)); bp.topMargin = Ui.dp(this,10); hero.addView(mappingButton,bp);
        body.addView(hero);

        body.addView(sectionTitle(getString(R.string.quick_actions)));
        LinearLayout quick = new LinearLayout(this);
        TextView launch = compactAction(getString(R.string.launch_game)); launch.setOnClickListener(v -> launchActiveGame(false));
        TextView edit = compactAction(getString(R.string.edit_overlay)); edit.setOnClickListener(v -> launchActiveGame(true));
        quick.addView(launch, new LinearLayout.LayoutParams(0,Ui.dp(this,66),1));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0,Ui.dp(this,66),1); ep.leftMargin=Ui.dp(this,10); quick.addView(edit,ep);
        body.addView(quick);

        body.addView(sectionTitle(getString(R.string.status)));
        body.addView(statusCard(getString(R.string.controller), controllerSummary(), null));
        body.addView(statusCard(getString(R.string.shizuku), shizukuSummary(), v -> onShizukuAction()));
        body.addView(statusCard(getString(R.string.accessibility), accessibilitySummary(), v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        return pageShell("Zploy", body);
    }

    private View gamesPage() {
        LinearLayout body = Ui.vertical(this);
        TextView add = Ui.button(this, "+  " + getString(R.string.add_game), true);
        add.setOnClickListener(v -> chooseInstalledGame());
        body.addView(add, new LinearLayout.LayoutParams(-1,Ui.dp(this,54)));
        TextView hint = Ui.text(this,getString(R.string.games_hint),11,Ui.GRAY,false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1,-2); hp.topMargin=Ui.dp(this,12); hp.bottomMargin=Ui.dp(this,18); body.addView(hint,hp);
        for (ProfileStore.Profile profile : profileStore.load()) body.addView(gameCard(profile));
        return pageShell(getString(R.string.games), body);
    }

    private View gameCard(ProfileStore.Profile profile) {
        LinearLayout card = Ui.vertical(this);
        card.setPadding(Ui.dp(this,17),Ui.dp(this,16),Ui.dp(this,17),Ui.dp(this,16));
        card.setBackground(Ui.round(Ui.WHITE,26,this));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout copy = Ui.vertical(this);
        copy.addView(Ui.text(this, profile.name, 15, Ui.BLACK, true));
        copy.addView(Ui.text(this,profile.isBound() ? profile.packageName : getString(R.string.not_bound),10,Ui.GRAY,false));
        top.addView(copy,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));
        TextView active = Ui.text(this, profile.id.equals(profileStore.activeId()) ? getString(R.string.active) : getString(R.string.use), 10, Ui.BLACK, true);
        active.setGravity(Gravity.CENTER); active.setBackground(Ui.round(Ui.LIGHT,13,this));
        active.setOnClickListener(v->{ profileStore.setActive(profile.id); notifyProfileChanged(); showPage(1); });
        top.addView(active,new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,36)));
        card.addView(top);

        String old = profileStore.activeId(); profileStore.setActive(profile.id); int count = new MappingStore(this).load().size(); profileStore.setActive(old);
        card.addView(Ui.text(this,getString(R.string.mapping_count,count),10,Ui.GRAY,false),new LinearLayout.LayoutParams(-1,Ui.dp(this,30)));

        LinearLayout actions = new LinearLayout(this);
        TextView launch = tinyAction(getString(R.string.launch)); launch.setOnClickListener(v->{profileStore.setActive(profile.id);notifyProfileChanged();launchActiveGame(false);});
        TextView edit = tinyAction(getString(R.string.edit)); edit.setOnClickListener(v->{profileStore.setActive(profile.id);notifyProfileChanged();launchActiveGame(true);});
        TextView more = tinyAction(getString(R.string.manage)); more.setOnClickListener(v->profileManageDialog(profile));
        actions.addView(launch,new LinearLayout.LayoutParams(0,Ui.dp(this,40),1));
        LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,Ui.dp(this,40),1); p2.leftMargin=Ui.dp(this,8); actions.addView(edit,p2);
        LinearLayout.LayoutParams p3=new LinearLayout.LayoutParams(0,Ui.dp(this,40),1); p3.leftMargin=Ui.dp(this,8); actions.addView(more,p3);
        card.addView(actions);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=Ui.dp(this,10); card.setLayoutParams(cp);
        return card;
    }

    private View controllerPage() {
        LinearLayout body = Ui.vertical(this);
        ControllerState state = ControllerStore.get().snapshot();
        LinearLayout hero = Ui.vertical(this);
        hero.setPadding(Ui.dp(this,20),Ui.dp(this,18),Ui.dp(this,20),Ui.dp(this,18)); hero.setBackground(Ui.round(Ui.BLACK,30,this));
        hero.addView(Ui.text(this,getString(R.string.controller),11,Color.rgb(155,155,155),false));
        controllerStatus = Ui.text(this,state.deviceName.isEmpty()?getString(R.string.not_connected):state.deviceName,24,Color.WHITE,true);
        hero.addView(controllerStatus,new LinearLayout.LayoutParams(-1,Ui.dp(this,52)));
        hero.addView(Ui.text(this,state.deviceName.isEmpty()?getString(R.string.connect_controller_hint):getString(R.string.input_detected),10,Color.rgb(155,155,155),false));
        TextView bluetooth = Ui.button(this,getString(R.string.bluetooth_devices),false); bluetooth.setOnClickListener(v->BluetoothControllerManager.openBluetoothSettings(this));
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,Ui.dp(this,50)); blp.topMargin=Ui.dp(this,14); hero.addView(bluetooth,blp); body.addView(hero);

        body.addView(sectionTitle(getString(R.string.paired_devices)));
        if (!BluetoothControllerManager.hasConnectPermission(this)) {
            TextView allow=Ui.button(this,getString(R.string.allow_bluetooth),true);allow.setOnClickListener(v->BluetoothControllerManager.requestPermission(this));body.addView(allow,new LinearLayout.LayoutParams(-1,Ui.dp(this,50)));
        } else {
            List<BluetoothControllerManager.DeviceEntry> devices=BluetoothControllerManager.bonded(this);
            if(devices.isEmpty())body.addView(emptyCard(getString(R.string.no_paired_devices)));
            else for(BluetoothControllerManager.DeviceEntry d:devices)body.addView(statusCard(d.name,d.address,v->BluetoothControllerManager.openBluetoothSettings(this)));
        }

        body.addView(sectionTitle(getString(R.string.controller_test)));
        ControllerView view=new ControllerView(this);view.setBackground(Ui.round(Ui.WHITE,28,this));body.addView(view,new LinearLayout.LayoutParams(-1,Ui.dp(this,290)));
        body.addView(sectionTitle(getString(R.string.raw_input)));
        rawInput=Ui.text(this,rawInputText(state),11,Ui.BLACK,false);rawInput.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16));rawInput.setBackground(Ui.round(Ui.WHITE,24,this));body.addView(rawInput,new LinearLayout.LayoutParams(-1,-2));
        return pageShell(getString(R.string.controller),body);
    }

    private View settingsPage() {
        LinearLayout body=Ui.vertical(this);
        body.addView(sectionTitle(getString(R.string.services)));
        body.addView(statusCard(getString(R.string.shizuku),shizukuSummary(),v->onShizukuAction()));
        body.addView(statusCard(getString(R.string.accessibility),accessibilitySummary(),v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        body.addView(sectionTitle(getString(R.string.preferences)));
        body.addView(settingRow(getString(R.string.language),currentLanguage(),v->languageDialog()));
        body.addView(settingRow(getString(R.string.backend),backendName(),v->backendDialog()));
        LinearLayout alpha=Ui.vertical(this);alpha.setPadding(Ui.dp(this,17),Ui.dp(this,15),Ui.dp(this,17),Ui.dp(this,15));alpha.setBackground(Ui.round(Ui.WHITE,25,this));alpha.addView(Ui.text(this,getString(R.string.overlay_alpha),13,Ui.BLACK,true));
        SeekBar bar=new SeekBar(this);bar.setMax(55);bar.setProgress(Math.round((Prefs.overlayAlpha(this)-.10f)*100));bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){Prefs.setOverlayAlpha(MainActivity.this,.10f+p/100f);ZployAccessibilityService z=ZployAccessibilityService.getInstance();if(z!=null)z.refreshOverlay();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});alpha.addView(bar,new LinearLayout.LayoutParams(-1,Ui.dp(this,44)));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.bottomMargin=Ui.dp(this,10);body.addView(alpha,ap);
        body.addView(sectionTitle(getString(R.string.about)));body.addView(settingRow("Zploy",getString(R.string.version_label),v->{}));
        return pageShell(getString(R.string.settings),body);
    }

    private void chooseInstalledGame() {
        List<GameCatalog.AppEntry> apps=GameCatalog.load(this);if(apps.isEmpty()){toast(getString(R.string.no_apps_found));return;}String[] labels=new String[apps.size()];for(int i=0;i<apps.size();i++)labels[i]=apps.get(i).label;
        new AlertDialog.Builder(this).setTitle(getString(R.string.choose_game)).setItems(labels,(d,which)->{GameCatalog.AppEntry app=apps.get(which);ProfileStore.Profile p=profileStore.createForGame(app.packageName,app.label);profileStore.setActive(p.id);notifyProfileChanged();showPage(1);}).show();
    }

    private void profileManageDialog(ProfileStore.Profile profile) {
        String[] actions={getString(R.string.rename),getString(R.string.change_game),getString(R.string.delete)};
        new AlertDialog.Builder(this).setTitle(profile.name).setItems(actions,(d,which)->{if(which==0)renameProfileDialog(profile);else if(which==1){profileStore.setActive(profile.id);chooseInstalledGame();}else deleteProfile(profile);}).show();
    }

    private void renameProfileDialog(ProfileStore.Profile profile) {
        EditText input=new EditText(this);input.setSingleLine(true);input.setText(profile.name);input.setSelection(input.length());FrameLayout wrap=new FrameLayout(this);int pad=Ui.dp(this,20);wrap.setPadding(pad,0,pad,0);wrap.addView(input,new FrameLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setTitle(getString(R.string.rename)).setView(wrap).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.save),(d,w)->{profileStore.rename(profile.id,input.getText().toString());showPage(1);}).show();
    }

    private void deleteProfile(ProfileStore.Profile profile) {
        new AlertDialog.Builder(this).setTitle(getString(R.string.delete_game_profile)).setMessage(profile.name).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.delete),(d,w)->{if(!profileStore.delete(profile.id)){toast(getString(R.string.cannot_delete_last_profile));return;}mappingStore.clearProfile(profile.id);notifyProfileChanged();showPage(1);}).show();
    }

    private void launchActiveGame(boolean edit) {
        ProfileStore.Profile active=profileStore.active();if(!active.isBound()){toast(getString(R.string.add_game_first));showPage(1);return;}
        if(edit){ZployAccessibilityService z=ZployAccessibilityService.getInstance();if(z==null){toast(getString(R.string.need_accessibility));return;}if(!Prefs.mappingEnabled(this))z.applyMappingState(true);z.openEditor();}
        if(!GameCatalog.launch(this,active.packageName))toast(getString(R.string.launch_failed));
    }

    private void toggleMapping() {
        boolean enable=!Prefs.mappingEnabled(this);if(enable&&!profileStore.active().isBound()){toast(getString(R.string.add_game_first));showPage(1);return;}ZployAccessibilityService z=ZployAccessibilityService.getInstance();if(z==null){toast(getString(R.string.need_accessibility));return;}if(enable&&Prefs.BACKEND_SHIZUKU.equals(Prefs.backend(this))&&!ShizukuBridge.get().isReady()){toast(getString(R.string.need_shizuku));return;}z.applyMappingState(enable);toast(enable?getString(R.string.mapping_started):getString(R.string.mapping_stopped));showPage(0);
    }

    private void notifyProfileChanged(){ZployAccessibilityService z=ZployAccessibilityService.getInstance();if(z!=null)z.refreshOverlay();}
    private void onShizukuAction(){if(!ShizukuBridge.get().isBinderAlive()){toast(getString(R.string.start_shizuku_first));return;}ShizukuBridge.get().requestPermission();}
    private TextView sectionTitle(String s){TextView t=Ui.text(this,s,13,Ui.BLACK,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,Ui.dp(this,46));p.topMargin=Ui.dp(this,8);t.setLayoutParams(p);return t;}
    private TextView compactAction(String s){TextView t=Ui.text(this,s,12,Ui.BLACK,true);t.setGravity(Gravity.CENTER);t.setBackground(Ui.round(Ui.WHITE,22,this));return t;}
    private TextView tinyAction(String s){TextView t=Ui.text(this,s,11,Ui.BLACK,true);t.setGravity(Gravity.CENTER);t.setBackground(Ui.round(Ui.LIGHT,14,this));return t;}
    private View emptyCard(String text){TextView t=Ui.text(this,text,12,Ui.GRAY,false);t.setGravity(Gravity.CENTER);t.setBackground(Ui.round(Ui.WHITE,24,this));FrameLayout f=new FrameLayout(this);f.addView(t,new FrameLayout.LayoutParams(-1,-1));f.setLayoutParams(new LinearLayout.LayoutParams(-1,Ui.dp(this,72)));return f;}

    private View statusCard(String title,String sub,View.OnClickListener click){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Ui.dp(this,17),Ui.dp(this,14),Ui.dp(this,14),Ui.dp(this,14));row.setBackground(Ui.round(Ui.WHITE,25,this));LinearLayout copy=Ui.vertical(this);copy.addView(Ui.text(this,title,13,Ui.BLACK,true));copy.addView(Ui.text(this,sub,10,Ui.GRAY,false));row.addView(copy,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));if(click!=null){TextView b=Ui.text(this,getString(R.string.open),10,Ui.BLACK,true);b.setGravity(Gravity.CENTER);b.setBackground(Ui.round(Ui.LIGHT,13,this));row.addView(b,new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,36)));row.setOnClickListener(click);}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=Ui.dp(this,9);row.setLayoutParams(p);return row;}
    private View settingRow(String title,String sub,View.OnClickListener click){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Ui.dp(this,17),Ui.dp(this,14),Ui.dp(this,17),Ui.dp(this,14));row.setBackground(Ui.round(Ui.WHITE,25,this));LinearLayout copy=Ui.vertical(this);copy.addView(Ui.text(this,title,13,Ui.BLACK,true));copy.addView(Ui.text(this,sub,10,Ui.GRAY,false));row.addView(copy,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1));row.addView(Ui.text(this,"›",24,Ui.GRAY,false),new LinearLayout.LayoutParams(Ui.dp(this,28),Ui.dp(this,50)));row.setOnClickListener(click);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=Ui.dp(this,10);row.setLayoutParams(p);return row;}

    private void detectController(){for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&ControllerStore.isControllerSource(d.getSources())){ControllerStore.get().setDevice(d);return;}}}
    private String controllerSummary(){ControllerState s=ControllerStore.get().snapshot();return s.deviceName.isEmpty()?getString(R.string.not_connected):s.deviceName;}
    private String shizukuSummary(){if(ShizukuBridge.get().isReady())return getString(R.string.shizuku_ready);if(ShizukuBridge.get().isBinderAlive())return getString(R.string.shizuku_waiting);return getString(R.string.shizuku_missing);}
    private String accessibilitySummary(){return ZployAccessibilityService.getInstance()!=null?getString(R.string.accessibility_ready):getString(R.string.accessibility_missing);}
    private String rawInputText(ControllerState s){return String.format(Locale.US,"Device  %s\nLX  %+.3f   LY  %+.3f\nRX  %+.3f   RY  %+.3f\nLT  %.3f   RT  %.3f\nHAT  %+.1f / %+.1f\nKey  %s (%d)",s.deviceName.isEmpty()?"—":s.deviceName,s.lx,s.ly,s.rx,s.ry,s.lt,s.rt,s.hatX,s.hatY,KeyEvent.keyCodeToString(s.lastKeyCode),s.lastKeyCode);}
    private String currentLanguage(){String tag=getResources().getConfiguration().getLocales().get(0).toLanguageTag();return tag.startsWith("zh")?getString(R.string.chinese):getString(R.string.english);}
    private void languageDialog(){String[] names={getString(R.string.follow_system),getString(R.string.chinese),getString(R.string.english)};new AlertDialog.Builder(this).setTitle(getString(R.string.language)).setItems(names,(d,w)->{Prefs.setLanguage(this,w==0?"":w==1?"zh-CN":"en");recreate();}).show();}
    private String backendName(){String b=Prefs.backend(this);if(Prefs.BACKEND_SHIZUKU.equals(b))return getString(R.string.backend_shizuku);if(Prefs.BACKEND_ACCESSIBILITY.equals(b))return getString(R.string.backend_accessibility);return getString(R.string.backend_auto);}
    private void backendDialog(){String[] names={getString(R.string.backend_auto),getString(R.string.backend_shizuku),getString(R.string.backend_accessibility)};new AlertDialog.Builder(this).setTitle(getString(R.string.backend)).setItems(names,(d,w)->{Prefs.setBackend(this,w==0?Prefs.BACKEND_AUTO:w==1?Prefs.BACKEND_SHIZUKU:Prefs.BACKEND_ACCESSIBILITY);showPage(3);}).show();}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}

    @Override public void onControllerState(ControllerState state){runOnUiThread(()->{if(rawInput!=null)rawInput.setText(rawInputText(state));if(controllerStatus!=null)controllerStatus.setText(state.deviceName.isEmpty()?getString(R.string.not_connected):state.deviceName);});}
    @Override public void onShizukuStateChanged(){runOnUiThread(()->{if(page==0||page==3)showPage(page);});}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==BluetoothControllerManager.REQUEST_CONNECT_PERMISSION)showPage(2);}
}
