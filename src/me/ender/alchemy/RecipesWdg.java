package me.ender.alchemy;

import haven.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class RecipesWdg extends Widget {
    private final ElixirWdg elixir;


    RecipesWdg() {
	ElixirList list = new ElixirList(this::onSelectionChanged);
	Coord p = add(list, AlchemyWnd.PAD, AlchemyWnd.PAD).pos("ur");

	elixir = add(new ElixirWdg(AlchemyWnd.CONTENT_W, list.sz.y), p.addx(AlchemyWnd.GAP));

	pack();
    }

    private void onSelectionChanged(Elixir elixir) {
	this.elixir.update(elixir);
    }

    private static class ElixirList extends FilteredListBox<Elixir> {
	private final Map<String, RichText> names = new HashMap<>();
	private final Consumer<Elixir> onChanged;
	private boolean dirty = true;

	public ElixirList(Consumer<Elixir> onChanged) {
	    super(AlchemyWnd.LIST_W, AlchemyWnd.ITEMS, AlchemyWnd.ITEM_H);
	    bgcolor = AlchemyWnd.BGCOLOR;
	    this.onChanged = onChanged;
	    listen(AlchemyData.ELIXIRS_UPDATED, this::onElixirsUpdated);
	}

	private void onElixirsUpdated() {
	    Elixir was = sel;
	    update();
	    change(was);
	}

	@Override
	public void changed(Elixir item, int index) {
	    onChanged.accept(item);
	}

	private void update() {
	    if(tvisible()) {
		setItems(AlchemyData.elixirs());
		dirty = false;
	    } else {
		dirty = true;
	    }
	}

	@Override
	public void draw(GOut g, boolean strict) {
	    if(dirty) {update();}
	    super.draw(g, strict);
	}

	private RichText text(Elixir elixir) {
	    String name = elixir.name();
	    RichText text = names.getOrDefault(name, null);
	    if(text != null) {return text;}

	    text = RichText.stdfrem.render(String.format("$img[%s,h=16,c] %s", elixir.recipe.res, name), AlchemyWnd.CONTENT_W);
	    names.put(name, text);
	    return text;
	}

	@Override
	public void dispose() {
	    names.values().forEach(Text::dispose);
	    names.clear();
	    super.dispose();
	}

	@Override
	protected boolean match(Elixir item, String text) {
	    if(text == null || text.isEmpty()) {return true;}

	    final String filter = text.toLowerCase();
	    return item.name().toLowerCase().contains(filter)
		|| item.effects.stream().anyMatch(e -> e.matches(filter))
		|| item.recipe.matches(filter);
	}

	@Override
	public void draw(GOut g) {
	    super.draw(g);
	}

	@Override
	protected void drawitem(GOut g, Elixir item, int i) {
	    g.image(text(item).tex(), Coord.z);
	}
    }
}