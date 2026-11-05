package net.atlasvisuals.gui;

import java.util.ArrayList;
import java.util.List;
import net.atlasvisuals.config.VisualsConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

/** Shared layout for the settings pages: a title, scrolling label and widget rows, and a Done button. */
@Environment(EnvType.CLIENT)
abstract class ScrollingScreen extends Screen {
   static final int ROW = 24;
   private static final int BOTTOM = 36;
   private static final int SCROLL_STEP = 16;
   private final Screen parent;
   private final List<Row> rows = new ArrayList<>();
   private double scroll;

   ScrollingScreen(Component title, Screen parent) {
      super(title);
      this.parent = parent;
   }

   /** Where the rows start, for pages with something fixed above them. */
   int top() {
      return 32;
   }

   /** Adds the page's rows through {@link #row}. */
   abstract void addRows();

   @Override
   protected void init() {
      this.rows.clear();
      this.addRows();
      this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
         .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
         .build());
      this.layoutRows();
   }

   final int contentLeft() {
      return Math.max(8, this.width / 2 - 190);
   }

   final int widgetLeft() {
      return this.width / 2 + 10;
   }

   final int widgetWidth() {
      return Math.min(180, this.width / 2 - 18);
   }

   final void row(String label, AbstractWidget... widgets) {
      for (AbstractWidget widget : widgets) {
         this.addRenderableWidget(widget);
      }

      this.rows.add(new Row(label, this.top() + this.rows.size() * ROW, widgets));
   }

   /** Rebuilds the page, keeping the scroll position, for when a change shows or hides other settings. */
   final void rebuild() {
      this.rebuildWidgets();
   }

   final void scrollToTop() {
      this.scroll = 0.0;
   }

   private int maxScroll() {
      int content = this.rows.size() * ROW;
      int visible = this.height - this.top() - BOTTOM;
      return Math.max(0, content - visible);
   }

   private void layoutRows() {
      this.scroll = Math.max(0.0, Math.min(this.scroll, this.maxScroll()));
      int bottom = this.height - BOTTOM;
      for (Row row : this.rows) {
         int y = row.y - (int)this.scroll;
         boolean shown = y >= this.top() && y + 20 <= bottom;
         for (AbstractWidget widget : row.widgets) {
            widget.setY(y);
            widget.visible = shown;
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
      this.scroll -= vertical * SCROLL_STEP;
      this.layoutRows();
      return true;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      super.extractRenderState(graphics, mouseX, mouseY, delta);
      graphics.centeredText(this.font, this.title, this.width / 2, 12, -1);
      int bottom = this.height - BOTTOM;
      for (Row row : this.rows) {
         int y = row.y - (int)this.scroll;
         if (y >= this.top() && y + 20 <= bottom) {
            graphics.text(this.font, row.label, this.contentLeft(), y + 6, -1);
            this.renderRow(graphics, row.label, y);
         }
      }

      int max = this.maxScroll();
      if (max > 0) {
         int top = this.top();
         int track = bottom - top;
         int content = this.rows.size() * ROW;
         int thumb = Math.max(16, track * track / content);
         int thumbTop = top + (int)((track - thumb) * this.scroll / max);
         int x = this.width - 6;
         graphics.fill(x, top, x + 3, bottom, 0x40FFFFFF);
         graphics.fill(x, thumbTop, x + 3, thumbTop + thumb, 0xC0FFFFFF);
      }
   }

   /** Extra per-row drawing, such as a colour swatch. */
   void renderRow(GuiGraphicsExtractor graphics, String label, int y) {
   }

   @Override
   public void onClose() {
      VisualsConfig.save();
      this.minecraft.gui.setScreen(this.parent);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private record Row(String label, int y, AbstractWidget[] widgets) {
   }
}
