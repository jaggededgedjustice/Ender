package me.ender.ui;

import haven.TextEntry;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ValueEntry extends TextEntry {
    private static final Set<Integer> ALLOWED_KEYS = new HashSet<>(Arrays.asList(
	KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
	KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9,
	KeyEvent.VK_NUMPAD0, KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, KeyEvent.VK_NUMPAD4,
	KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, KeyEvent.VK_NUMPAD7, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9,
	KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
	KeyEvent.VK_ENTER, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE
    ));
    private final Runnable action;
    public boolean clearOnFocus;

    public ValueEntry(int w, String deftext, Runnable action) {
	super(w, deftext);
	this.action = action;
	canactivate = true;
    }

    @Override
    public void gotfocus() {
	super.gotfocus();
	if(clearOnFocus) {settext("");}
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
	if("activate".equals(msg) && action != null) {
	    action.run();
	} else {
	    super.wdgmsg(msg, args);
	}
    }

    @Override
    public boolean keydown(KeyDownEvent ev) {
	int keyCode = ev.code;
	if(keyCode == 0) {
	    keyCode = ev.awt.getKeyChar();
	}
	if(ALLOWED_KEYS.contains(keyCode)) {
	    return super.keydown(ev);
	}
	return false;
    }

    public int value() {
	try {
	    return Integer.parseInt(text());
	} catch (Exception ignored) {
	}
	return -1;
    }

}
