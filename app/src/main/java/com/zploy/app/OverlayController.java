package com.zploy.app;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Game-bound floating editor. Nothing is shown outside the selected game. */
public final class OverlayController {
    private final ZployAccessibilityService service;
    private final WindowManager wm;
    private final MappingStore store;
    private final ProfileStore profiles;

    private TextView bubble;
    private FrameLayout overlay;
    private boolean bubbleShown;
    private boolean overlayShown;
    private boolean editing;
    private boolean enabled;
    private boolean editorRequested;
    private boolean choosingType;
    private String foregroundPackage = "";
    private MappingItem selected;
    private MappingType pendingType;
    private float bubbleDownX, bubbleDownY;
    private int bubbleStartX, bubbleStartY;

    public OverlayController(ZployAccessibilityService service) {
        this.service = service;
        this.wm = service.getSystemService(WindowManager.class);
        this.store = new MappingStore(service);
        this.profiles = new ProfileStore(service);
    }

    public boolean isEditing() { return editing; }
    public boolean isTargetGameForeground() {
        String target = profiles.activePackage();
        return !target.isEmpty() && target.equals(foregroundPackage);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        syncVisibility();
    }

    public void onForegroundPackageChanged(String packageName) {
        foregroundPackage = packageName == null ? "" : packageName;
        ProfileStore.Profile matched = profiles.findByPackage(foregroundPackage);
        if (matched != null && !matched.id.equals(profiles.activeId())) profiles.setActive(matched.id);
        syncVisibility();
    }

    public void openEditor() {
        editorRequested = true;
        if (isTargetGameForeground()) showMappingOverlay(true);
    }

    public void toggleEditor() {
        if (isTargetGameForeground()) showMappingOverlay(!editing);
    }

    public void onControllerKeyPressed(int keyCode) {
        if (!editing || pendingType == null || keyCode == KeyEvent.KEYCODE_UNKNOWN) return;
        if (pendingType != MappingType.TAP && pendingType != MappingType.HOLD) return;
        List<MappingItem> items = store.load();
        MappingItem found = null;
        for (MappingItem item : items) if (item.keyCode == keyCode) { found = item; break; }
        if (found == null) {
            found = MappingStore.newButton(keyCode, pendingType);
            items.add(found);
        } else {
            found.type = pendingType;
        }
        store.save(items);
        selected = found;
        pendingType = null;
        choosingType = false;
        renderOverlay();
    }

    public void refresh() {
        syncVisibility();
        if (overlayShown) renderOverlay();
    }

    public void destroy() { removeOverlay(); removeBubble(); }

    private void syncVisibility() {
        if (!enabled || !isTargetGameForeground()) {
            editing = false;
            pendingType = null;
            choosingType = false;
            removeOverlay();
            removeBubble();
            return;
        }
        showBubble();
        if (editorRequested) {
            editorRequested = false;
            showMappingOverlay(true);
        } else if (!overlayShown) {
            showMappingOverlay(false);
        }
    }

    private WindowManager.LayoutParams bubbleParams() {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                Ui.dp(service, 46), Ui.dp(service, 46),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = Ui.dp(service, 12);
        p.y = Ui.dp(service, 120);
        return p;
    }

    private void showBubble() {
        if (bubbleShown || wm == null) return;
        bubble = Ui.text(service, "Z", 14, Color.WHITE, true);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(Ui.round(Color.rgb(25,25,25),18,service));
        bubble.setAlpha(0.82f);
        bubble.setOnTouchListener((v,event)->{
            WindowManager.LayoutParams lp=(WindowManager.LayoutParams)bubble.getLayoutParams();
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    bubbleDownX=event.getRawX();bubbleDownY=event.getRawY();bubbleStartX=lp.x;bubbleStartY=lp.y;return true;
                case MotionEvent.ACTION_MOVE:
                    lp.x=bubbleStartX+Math.round(event.getRawX()-bubbleDownX);lp.y=bubbleStartY+Math.round(event.getRawY()-bubbleDownY);wm.updateViewLayout(bubble,lp);return true;
                case MotionEvent.ACTION_UP:
                    float dist=Math.abs(event.getRawX()-bubbleDownX)+Math.abs(event.getRawY()-bubbleDownY);if(dist<Ui.dp(service,10))toggleEditor();return true;
            }
            return false;
        });
        wm.addView(bubble,bubbleParams());
        bubbleShown=true;
    }

    private WindowManager.LayoutParams overlayParams(boolean edit) {
        int flags=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if(!edit)flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,flags,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.START;
        return p;
    }

    private void showMappingOverlay(boolean edit) {
        if(!isTargetGameForeground())return;
        editing=edit;
        if(!edit){selected=null;pendingType=null;choosingType=false;}
        if(overlayShown){wm.updateViewLayout(overlay,overlayParams(edit));renderOverlay();return;}
        overlay=new FrameLayout(service);overlay.setBackgroundColor(Color.TRANSPARENT);wm.addView(overlay,overlayParams(edit));overlayShown=true;renderOverlay();
    }

    private void renderOverlay() {
        if(!overlayShown||overlay==null)return;
        overlay.removeAllViews();
        List<MappingItem> items=new ArrayList<>(store.load());
        Rect bounds=wm.getCurrentWindowMetrics().getBounds();
        int width=bounds.width(),height=bounds.height();
        float playAlpha=Prefs.overlayAlpha(service);

        for(MappingItem item:items){
            boolean analog=item.type==MappingType.CAMERA||item.type==MappingType.JOYSTICK;
            TextView marker=Ui.text(service,item.label,analog?12:13,Color.WHITE,true);marker.setGravity(Gravity.CENTER);
            int size=Ui.dp(service,analog?64:46);
            int bg=selected!=null&&selected.id.equals(item.id)&&editing?Color.argb(235,17,17,17):Color.argb(210,45,45,45);
            marker.setBackground(Ui.round(bg,analog?32:18,service));marker.setAlpha(editing?0.92f:playAlpha);
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(size,size);lp.leftMargin=Math.round(item.x*width-size/2f);lp.topMargin=Math.round(item.y*height-size/2f);marker.setLayoutParams(lp);
            if(editing)marker.setOnTouchListener(new View.OnTouchListener(){float downX,downY;int startL,startT;boolean moved;@Override public boolean onTouch(View v,MotionEvent e){FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)v.getLayoutParams();switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startL=p.leftMargin;startT=p.topMargin;moved=false;return true;case MotionEvent.ACTION_MOVE:int dx=Math.round(e.getRawX()-downX),dy=Math.round(e.getRawY()-downY);if(Math.abs(dx)+Math.abs(dy)>Ui.dp(service,4))moved=true;p.leftMargin=startL+dx;p.topMargin=startT+dy;v.setLayoutParams(p);return true;case MotionEvent.ACTION_UP:if(moved){item.x=MappingMath.clamp((p.leftMargin+size/2f)/width,0f,1f);item.y=MappingMath.clamp((p.topMargin+size/2f)/height,0f,1f);replaceAndSave(item);}selected=item;pendingType=null;choosingType=false;renderOverlay();return true;}return false;}});
            overlay.addView(marker);
        }
        if(editing)addEditorPanel();
    }

    private void addEditorPanel() {
        LinearLayout panel=Ui.vertical(service);panel.setPadding(Ui.dp(service,12),Ui.dp(service,10),Ui.dp(service,12),Ui.dp(service,10));panel.setBackground(Ui.round(Color.argb(242,17,17,17),22,service));
        String title=choosingType?service.getString(R.string.choose_mapping_type):pendingType!=null?(pendingType==MappingType.TAP||pendingType==MappingType.HOLD?service.getString(R.string.press_controller_button):service.getString(R.string.choose_stick)):selected!=null?selected.label+" · "+typeText(selected.type):profiles.activeName();
        panel.addView(Ui.text(service,title,12,Color.WHITE,true),new LinearLayout.LayoutParams(-2,Ui.dp(service,32)));
        LinearLayout actions=new LinearLayout(service);actions.setOrientation(LinearLayout.HORIZONTAL);
        if(choosingType){
            addPanelButton(actions,service.getString(R.string.tap),()->selectPendingType(MappingType.TAP));
            addPanelButton(actions,service.getString(R.string.hold),()->selectPendingType(MappingType.HOLD));
            addPanelButton(actions,service.getString(R.string.joystick),()->selectPendingType(MappingType.JOYSTICK));
            addPanelButton(actions,service.getString(R.string.camera),()->selectPendingType(MappingType.CAMERA));
            addPanelButton(actions,service.getString(R.string.cancel),()->{choosingType=false;renderOverlay();});
        }else if(pendingType==null&&selected==null){
            addPanelButton(actions,"+ "+service.getString(R.string.add_mapping),()->{choosingType=true;renderOverlay();});
            addPanelButton(actions,service.getString(R.string.done),()->showMappingOverlay(false));
        }else if(pendingType!=null){
            if(pendingType==MappingType.JOYSTICK||pendingType==MappingType.CAMERA){
                addPanelButton(actions,"LS",()->createAnalog(MappingItem.KEY_LEFT_STICK,pendingType));
                addPanelButton(actions,"RS",()->createAnalog(MappingItem.KEY_RIGHT_STICK,pendingType));
            }
            addPanelButton(actions,service.getString(R.string.cancel),()->{pendingType=null;renderOverlay();});
        }else{
            if(selected.type==MappingType.TAP||selected.type==MappingType.HOLD){addPanelButton(actions,service.getString(R.string.tap),()->changeSelectedType(MappingType.TAP));addPanelButton(actions,service.getString(R.string.hold),()->changeSelectedType(MappingType.HOLD));}
            addPanelButton(actions,service.getString(R.string.delete),this::deleteSelected);
            addPanelButton(actions,service.getString(R.string.back),()->{selected=null;renderOverlay();});
        }
        panel.addView(actions);
        if(selected!=null&&(selected.type==MappingType.JOYSTICK||selected.type==MappingType.CAMERA)){
            addSlider(panel,service.getString(R.string.sensitivity),30,200,Math.round(selected.sensitivity*100f),v->{selected.sensitivity=v/100f;replaceAndSave(selected);});
            addSlider(panel,service.getString(R.string.dead_zone),0,30,Math.round(selected.deadZone*100f),v->{selected.deadZone=v/100f;replaceAndSave(selected);});
            if(selected.type==MappingType.JOYSTICK)addSlider(panel,service.getString(R.string.radius),3,18,Math.round(selected.radius*100f),v->{selected.radius=v/100f;replaceAndSave(selected);});
        }
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,-2);p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;p.topMargin=Ui.dp(service,18);overlay.addView(panel,p);
    }

    private void selectPendingType(MappingType type){choosingType=false;pendingType=type;selected=null;renderOverlay();}
    private void createAnalog(int keyCode,MappingType type){if(type!=MappingType.JOYSTICK&&type!=MappingType.CAMERA)return;List<MappingItem> items=store.load();for(MappingItem i:items){if(i.keyCode==keyCode){i.type=type;selected=i;pendingType=null;store.save(items);renderOverlay();return;}}MappingItem item=MappingStore.newAnalog(keyCode,type);items.add(item);store.save(items);selected=item;pendingType=null;renderOverlay();}
    private interface IntValueListener{void onValue(int value);}
    private void addSlider(LinearLayout parent,String label,int min,int max,int current,IntValueListener listener){LinearLayout block=Ui.vertical(service);block.setPadding(0,Ui.dp(service,7),0,0);block.addView(Ui.text(service,label,10,Color.rgb(190,190,190),false));SeekBar bar=new SeekBar(service);bar.setMax(max-min);bar.setProgress(Math.max(0,Math.min(max-min,current-min)));bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){if(f)listener.onValue(p+min);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});block.addView(bar,new LinearLayout.LayoutParams(Ui.dp(service,300),Ui.dp(service,38)));parent.addView(block);}
    private void addPanelButton(LinearLayout row,String label,Runnable action){TextView b=Ui.text(service,label,11,Color.WHITE,false);b.setGravity(Gravity.CENTER);b.setBackground(Ui.round(Color.rgb(45,45,45),14,service));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,Ui.dp(service,38));bp.rightMargin=Ui.dp(service,6);b.setPadding(Ui.dp(service,14),0,Ui.dp(service,14),0);row.addView(b,bp);b.setOnClickListener(v->action.run());}
    private void changeSelectedType(MappingType type){if(selected==null)return;selected.type=type;replaceAndSave(selected);renderOverlay();}
    private void deleteSelected(){if(selected==null)return;List<MappingItem> current=store.load();current.removeIf(m->m.id.equals(selected.id));store.save(current);selected=null;renderOverlay();}
    private String typeText(MappingType type){switch(type){case HOLD:return service.getString(R.string.hold);case JOYSTICK:return service.getString(R.string.joystick);case CAMERA:return service.getString(R.string.camera);default:return service.getString(R.string.tap);}}
    private void replaceAndSave(MappingItem changed){List<MappingItem> current=store.load();for(int i=0;i<current.size();i++)if(current.get(i).id.equals(changed.id)){current.set(i,changed);break;}store.save(current);}
    private void removeOverlay(){if(overlayShown&&overlay!=null&&wm!=null)try{wm.removeView(overlay);}catch(Exception ignored){}overlayShown=false;overlay=null;}
    private void removeBubble(){if(bubbleShown&&bubble!=null&&wm!=null)try{wm.removeView(bubble);}catch(Exception ignored){}bubbleShown=false;bubble=null;}
}
