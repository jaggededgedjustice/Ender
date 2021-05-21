package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ExtInventory extends Widget implements DTarget {
    private static final int margin = UI.scale(5);
    private static final int listw = UI.scale(125);
    private static final int itemh = UI.scale(20);
    private static final Color even = new Color(255, 255, 255, 16);
    private static final Color odd = new Color(255, 255, 255, 32);
    private static final Color found = new Color(255, 255, 0, 32);
    private static final String CFG_GROUP = "ext.group";
    private static final String CFG_SHOW = "ext.show";
    private static final String CFG_INV = "ext.inv";
    private static final Set<String> EXCLUDES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("Steelbox", "Pouch", "Frame", "Tub", "Fireplace", "Rack", "Pane mold", "Table", "Purse")));
    public final Inventory inv;
    private final ItemGroupList list;
    private final Widget extension;
    private final Label space;
    private SortedMap<ItemType, List<WItem>> groups;
    private final Dropbox<Grouping> grouping;
    private boolean disabled = false;
    private boolean showInv = true;
    private boolean needUpdate = false;
    private boolean once = true;
    private WindowX wnd;
    private final ICheckBox chb_show = new ICheckBox("gfx/hud/btn-extlist", "", "-d", "-h");
    
    public ExtInventory(Coord sz) {
	inv = new Inventory(sz);
	inv.ext = this;
	extension = new Widget();
	Composer composer = new Composer(extension).hmrgn(margin).vmrgn(margin);
	grouping = new Dropbox<Grouping>(UI.scale(75), 5, UI.scale(16)) {
	    {bgcolor = new Color(16, 16, 16, 128);}
	    
	    @Override
	    protected Grouping listitem(int i) {
		return Grouping.values()[i];
	    }
	    
	    @Override
	    protected int listitems() {
		return Grouping.values().length;
	    }
	    
	    @Override
	    protected void drawitem(GOut g, Grouping item, int i) {
		g.atext(item.name, UI.scale(3, 8), 0, 0.5);
	    }
	    
	    @Override
	    public void change(Grouping item) {
		if(item != sel && wnd != null) {
		    wnd.cfg.setValue(CFG_GROUP, item.name());
		    wnd.storeCfg();
		}
		needUpdate = true;
		super.change(item);
	    }
	};
	space = new Label("");
	grouping.sel = Grouping.NONE;
	composer.addr(new Label("Group:"), grouping);
	list = new ItemGroupList(listw, (inv.sz.y - composer.y() - 2 * margin - space.sz.y) / itemh, itemh);
	composer.add(list);
	composer.add(space);
	extension.pack();
	composer = new Composer(this).hmrgn(margin);
	composer.addr(inv, extension);
	pack();
    }
    
    public void hideExtension() {
	extension.hide();
	updateLayout();
    }
    
    public void showExtension() {
	extension.show();
	updateLayout();
    }
    
    public void disable() {
	hideExtension();
	disabled = true;
	chb_show.hide();
	if(wnd != null) {wnd.placetwdgs();}
    }
    
    @Override
    protected void added() {
	wnd = getparent(WindowX.class);
	if(wnd != null) {
	    disabled = disabled || needDisableExtraInventory(wnd.caption());
	    boolean vis = !disabled && wnd.cfg.getValue(CFG_SHOW, false);
	    showInv = wnd.cfg.getValue(CFG_INV, true);
	    if(!disabled) {
		chb_show.a = vis;
		wnd.addtwdg(wnd.add(chb_show)
		    .rclick(this::toggleInventory)
		    .changed(this::setVisibility)
		    .settip("LClick to toggle extra info\nRClick to hide inventory when info is visible", true)
		);
		grouping.sel = Grouping.valueOf(wnd.cfg.getValue(CFG_GROUP, Grouping.NONE.name()));
		needUpdate = true;
	    }
	    hideExtension();
	    extension.setfocus(list);
	}
    }
    
    private void setVisibility(boolean v) {
	if(wnd != null) {
	    wnd.cfg.setValue(CFG_SHOW, v);
	    wnd.storeCfg();
	}
	if(v) {
	    showExtension();
	} else {
	    hideExtension();
	}
    }
    
    private void toggleInventory() {
	showInv = !showInv;
	if(wnd != null) {
	    wnd.cfg.setValue(CFG_INV, showInv);
	    wnd.storeCfg();
	}
	updateLayout();
    }
    
    private void updateLayout() {
	inv.visible = showInv || !extension.visible;
	
	int szx = 0;
	int szy = inv.pos("br").y;
	if(inv.visible && parent != null) {
	    szx = inv.sz.x;
	    for (Widget w : wnd.children()) {
		if(w != this && (wnd != parent || w != wnd.cbtn && !wnd.twdgs.contains(w))) {
		    Position p = w.pos("br");
		    szx = Math.max(szx, p.x);
		    szy = Math.max(szy, p.y);
		}
	    }
	}
	extension.move(new Coord(szx + margin, extension.c.y));
	space.c.y = szy - space.sz.y;
	list.resize(new Coord(list.sz.x, space.c.y - grouping.sz.y - 2 * margin));
	extension.pack();
	pack();
	if(wnd != null) {wnd.pack();}
	if(showInv) {
	    chb_show.setTex("gfx/hud/btn-extlist", "", "-d", "-h");
	} else {
	    chb_show.setTex("gfx/hud/btn-extlist2", "", "-d", "-h");
	}
    }
    
    private void updateSpace() {
	String value = String.format("%d/%d", inv.filled(), inv.size());
	if(!value.equals(space.texts)) {
	    space.settext(value);
	}
    }
    
    @Override
    public boolean drop(Coord cc, Coord ul) {
	return (inv.drop(cc, ul));
    }
    
    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
	return (inv.iteminteract(cc, ul));
    }
    
    @Override
    public void addchild(Widget child, Object... args) {
	inv.addchild(child, args);
    }
    
    @Override
    public void cdestroy(Widget w) {
	super.cdestroy(w);
	inv.cdestroy(w);
    }
    
    public void itemsChanged() {
	needUpdate = true;
    }
    
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == inv) {
	    super.wdgmsg(this, msg, args);
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }
    
    @Override
    public void uimsg(String msg, Object... args) {
	if(msg.equals("sz") || msg.equals("mode")) {
	    int szx = inv.sz.x;
	    int szy = inv.sz.y;
	    inv.uimsg(msg, args);
	    if((szx != inv.sz.x) || (szy != inv.sz.y)) {
		updateLayout();
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
	super.mousewheel(c, amount);
	return(true);
    }

    @Override
    public void tick(double dt) {
	if(needUpdate && extension.visible) {
	    needUpdate = false;
	    SortedMap<ItemType, List<WItem>> groups = new TreeMap<>();
	    inv.forEachItem((g, w) -> {
		try {
		    Double quality = quality(w, grouping.sel);
		    ItemType type = new ItemType(name(w), quality);
		    if(type.loading) {needUpdate = true;}
		    groups.computeIfAbsent(type, k -> new ArrayList<>()).add(w);
		} catch (Loading ignored) {
		    needUpdate = true;
		}
	    });
	    this.groups = groups;
	}
	if(once) {
	    once = false;
	    if(!disabled && chb_show.a) {
		showExtension();
	    }
	}
	if(extension.visible) {
	    updateSpace();
	}
	super.tick(dt);
    }
    
    private static String name(WItem item) {
	return item.name.get("???");
    }
    
    private static Double quality(WItem item) {
	return quality(item, Grouping.Q);
    }
    
    private static Double quality(WItem item, Grouping g) {
	if(g == null || g == Grouping.NONE) {return null;}
	QualityList q = item.itemq.get();
	return (q == null || q.isEmpty()) ? null : quantifyQ(q.single().value, g);
    }
    
    private static Double quantifyQ(Double q, Grouping g) {
	if(q == null) {return null;}
	if(g == Grouping.Q1) {
	    q = Math.floor(q);
	} else if(g == Grouping.Q5) {
	    q = Math.floor(q);
	    q -= q % 5;
	} else if(g == Grouping.Q10) {
	    q = Math.floor(q);
	    q -= q % 10;
	}
	return q;
    }
    
    private static class ItemType implements Comparable<ItemType> {
	final String name;
	final Double quality;
	final boolean loading;

	public ItemType(String name, Double quality) {
	    this.name = name;
	    this.quality = quality;
	    loading = name.startsWith("???");
	}

	@Override
	public int compareTo(ItemType other) {
	    int byName = name.compareTo(other.name);
	    if((byName != 0) || (quality == null) || (other.quality == null)) {
		return(byName);
	    }
	    return(-Double.compare(quality, other.quality));
	}
    }
    
    private static class ItemsGroup extends Widget {
	private static final Map<String, Tex> cache = new WeakHashMap<>();
	private static final Color progc = new Color(31, 209, 185, 128);
	private static final BufferedImage def = WItem.missing.layer(Resource.imgc).img;
	private static final Text.Foundry foundry = new Text.Foundry(Text.sans, 12).aa(true);
	final ItemType type;
	final List<WItem> items;
	final WItem sample;
	private final Text.Line text;
	private Tex icon;
	
	public ItemsGroup(ItemType type, List<WItem> items, UI ui, Grouping g) {
	    super(new Coord(listw, itemh));
	    this.ui = ui;
	    this.type = type;
	    this.items = items;
	    items.sort(ExtInventory::byQuality);
	    this.sample = items.get(0);
	    double quality;
	    if(type.quality == null) {
		quality = items.stream().map(ExtInventory::quality).filter(Objects::nonNull).reduce(0.0, Double::sum)
		    / items.stream().map(ExtInventory::quality).filter(Objects::nonNull).count();
	    } else {
		quality = type.quality;
	    }
	    String format = (g == Grouping.NONE || g == Grouping.Q) ? "%sq%.1f (%d)" : "%sq%.0f+ (%d)";
	    this.text = foundry.render(String.format(format, type.quality != null ? "" : "avg ", quality, items.size()));
	}

	@Override
	public void draw(GOut g) {
	    if(icon == null) {
		if(cache.containsKey(type.name)) {
		    icon = cache.get(type.name);
		} else if(!type.loading) {
		    try {
			GSprite sprite = sample.item.sprite();
			if(sprite instanceof GSprite.ImageSprite) {
			    icon = GobIcon.SettingsWindow.Icon.tex(((GSprite.ImageSprite) sprite).image());
			} else {
			    Resource.Image image = sample.item.resource().layer(Resource.imgc);
			    if(image == null) {
				icon = GobIcon.SettingsWindow.Icon.tex(def);
			    } else {
				icon = GobIcon.SettingsWindow.Icon.tex(image.img);
			    }
			}
			cache.put(type.name, icon);
		    } catch (Loading ignored) {
		    }
		}
	    }
	    if(icon != null) {
		double meter = sample.meter();
		if(meter > 0) {
		    g.chcolor(progc);
		    g.frect(new Coord(icon.sz().x + margin, 0), new Coord((int) ((sz.x - icon.sz().x - margin) * meter), sz.y));
		    g.chcolor();
		}
		g.aimage(icon, new Coord(0, itemh / 2), 0.0, 0.5);
		g.aimage(text.tex(), new Coord(icon.sz().x + margin, itemh / 2), 0.0, 0.5);
	    } else {
		g.aimage(text.tex(), new Coord(margin, itemh / 2), 0.0, 0.5);
	    }
	}

	@Override
	public boolean mousedown(Coord c, int button) {
	    if(ui.modshift && (button == 1 || button == 3)) {
		process(items, "transfer", ui.modmeta, button == 3);
		return true;
	    } else if(ui.modctrl && (button == 1 || button == 3)) {
		process(items, "drop", ui.modmeta, button == 3);
		return true;
	    } else if(button == 1) {
		items.get(0).item.wdgmsg("take", Inventory.sqsz.div(2));
		return true;
	    } else if(button == 3) {
		items.get(0).item.wdgmsg("iact", Inventory.sqsz.div(2), ui.modflags());
		return true;
	    }
	    return (false);
	}
    
	private static void process(final List<WItem> items, String action, boolean all, boolean reverse) {
	    if(reverse) {
		items.sort(ExtInventory::byReverseQuality);
	    } else {
		items.sort(ExtInventory::byQuality);
	    }
	    if(!all) {
		items.get(0).item.wdgmsg(action, Inventory.sqsz.div(2), 1);
	    } else {
		for (WItem item : items) {
		    item.item.wdgmsg(action, Inventory.sqsz.div(2), 1);
		}
	    }
	}
	
	@Override
	public Object tooltip(Coord c, Widget prev) {
	    return(sample.tooltip(c, prev));
	}
    }
    
    private static int byReverseQuality(WItem a, WItem b) {
	return byQuality(b, a);
    }
    
    private static int byQuality(WItem a, WItem b) {
	Double qa = quality(a, Grouping.Q);
	Double qb = quality(b, Grouping.Q);
	if(Objects.equals(qa, qb)) {return 0;}
	if(qa == null) {return 1;}
	if(qb == null) {return -1;}
	return Double.compare(qb, qa);
    }
    
    public static boolean needDisableExtraInventory(String title) {
	return EXCLUDES.contains(title);
    }
    
    private class ItemGroupList extends Searchbox<ItemsGroup> {
	private List<ItemsGroup> groups = Collections.emptyList();

	public ItemGroupList(int w, int h, int itemh) {
	    super(w, h, itemh);
	}

	@Override
	protected boolean searchmatch(int idx, String text) {
	    return(groups.get(idx).type.name.toLowerCase().contains(text.toLowerCase()));
	}

	@Override
	protected ItemsGroup listitem(int i) {
	    return(groups.get(i));
	}

	@Override
	protected int listitems() {
	    return(groups.size());
	}

	@Override
	protected void drawitem(GOut g, ItemsGroup item, int i) {
	    if(soughtitem(i)) {
		g.chcolor(found);
		g.frect(Coord.z, g.sz());
	    }
	    g.chcolor(((i % 2) == 0) ? even : odd);
	    g.frect(Coord.z, g.sz());
	    g.chcolor();
	    item.draw(g);
	}

	@Override
	public void tick(double dt) {
	    if(ExtInventory.this.groups == null) {
		groups = Collections.emptyList();
	    } else {
		groups = ExtInventory.this.groups.entrySet().stream()
		    .map(v -> new ItemsGroup(v.getKey(), v.getValue(), ui, grouping.sel)).collect(Collectors.toList());
	    }
	    super.tick(dt);
	}
    
	@Override
	protected void drawbg(GOut g) {
	}
    
	@Override
	public Object tooltip(Coord c, Widget prev) {
	    int idx = idxat(c);
	    ItemsGroup item = (idx >= listitems()) ? null : listitem(idx);
	    if(item != null) {
		return item.tooltip(Coord.z, prev);
	    }
	    return super.tooltip(c, prev);
	}
    }
    
    public static Inventory inventory(Widget wdg) {
	if(wdg instanceof ExtInventory) {
	    return ((ExtInventory) wdg).inv;
	} else if(wdg instanceof Inventory) {
	    return (Inventory) wdg;
	} else {
	    return null;
	}
    }
    
    enum Grouping {
	NONE("Type"), Q("Quality"), Q1("Quality 1"), Q5("Quality 5"), Q10("Quality 10");
	
	private final String name;
	
	Grouping(String name) {this.name = name;}
    }
}
