/* Preprocessed source code */
package haven.res.ui.tt.tool;

import haven.*;
import static haven.PUtils.*;
import java.awt.image.*;
import java.awt.Graphics;
import java.util.*;

/* >tt: Tool */
@haven.FromResource(name = "ui/tt/tool", version = 1)
public class Tool extends ItemInfo.Tip {
    public static final Text.Line ch = Text.render("When used:");
    public final List<ItemInfo> sub;

    public Tool(Owner owner, List<ItemInfo> sub) {
	super(owner);
	this.sub = sub;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	Resource.Resolver rr = owner.context(Resource.Resolver.class);
	Object[] raw = Utils.splice(args, 1);
	return(new Tool(owner, buildinfo(owner, raw)));
    }

    public void prepare(Layout l) {
	l.intern(Collected.id).sub.addAll(sub);
    }

    public static class Collected extends Tip {
	public static final Layout.TipID<Collected> id = Collected::new;
	public final List<ItemInfo> sub = new ArrayList<>();

	public Collected(Owner owner) {
	    super(owner);
	}

	public void layout(Layout l) {
	    BufferedImage stip = longtip(sub);
	    if(stip != null) {
		l.cmp.add(ch.img, new Coord(0, l.cmp.sz.y));
		l.cmp.add(stip, new Coord(10, l.cmp.sz.y));
	    }
	}

	public int order() {return(90);}

	public static final String chc = "192,192,255";
    }
}
