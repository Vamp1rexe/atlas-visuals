package net.atlasvisuals.gui;

import java.util.ArrayList;
import java.util.List;
import net.atlasvisuals.config.VisualsConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_4185;
import net.minecraft.class_437;

/** Shared layout for the settings pages: a title, scrolling label and widget rows, and a Done button. */
@Environment(EnvType.CLIENT)
abstract class ScrollingScreen extends class_437 {
   static final int ROW = 24;
   private static final int BOTTOM = 36;
   private static final int SCROLL_STEP = 16;
   private final class_437 parent;
   private final List<Row> rows = new ArrayList<>();
   private double scroll;

   ScrollingScreen(class_2561 title, class_437 parent) {
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
   protected void method_25426() {
      this.rows.clear();
      this.addRows();
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Done"), button -> this.method_25419())
         .method_46434(this.field_22789 / 2 - 100, this.field_22790 - 28, 200, 20)
         .method_46431());
      this.layoutRows();
   }

   final int contentLeft() {
      return Math.max(8, this.field_22789 / 2 - 190);
   }

   final int widgetLeft() {
      return this.field_22789 / 2 + 10;
   }

   final int widgetWidth() {
      return Math.min(180, this.field_22789 / 2 - 18);
   }

   final void row(String label, class_339... widgets) {
      for (class_339 widget : widgets) {
         this.method_37063(widget);
      }

      this.rows.add(new Row(label, this.top() + this.rows.size() * ROW, widgets));
   }

   /** Rebuilds the page, keeping the scroll position, for when a change shows or hides other settings. */
   final void rebuild() {
      this.method_41843();
   }

   final void scrollToTop() {
      this.scroll = 0.0;
   }

   private int maxScroll() {
      int content = this.rows.size() * ROW;
      int visible = this.field_22790 - this.top() - BOTTOM;
      return Math.max(0, content - visible);
   }

   private void layoutRows() {
      this.scroll = Math.max(0.0, Math.min(this.scroll, this.maxScroll()));
      int bottom = this.field_22790 - BOTTOM;
      for (Row row : this.rows) {
         int y = row.y - (int)this.scroll;
         boolean shown = y >= this.top() && y + 20 <= bottom;
         for (class_339 widget : row.widgets) {
            widget.method_46419(y);
            widget.field_22764 = shown;
         }
      }
   }

   @Override
   public boolean method_25401(double mouseX, double mouseY, double horizontal, double vertical) {
      this.scroll -= vertical * SCROLL_STEP;
      this.layoutRows();
      return true;
   }

   @Override
   public void method_25394(class_332 graphics, int mouseX, int mouseY, float delta) {
      super.method_25394(graphics, mouseX, mouseY, delta);
      graphics.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 12, -1);
      int bottom = this.field_22790 - BOTTOM;
      for (Row row : this.rows) {
         int y = row.y - (int)this.scroll;
         if (y >= this.top() && y + 20 <= bottom) {
            graphics.method_25303(this.field_22793, row.label, this.contentLeft(), y + 6, -1);
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
         int x = this.field_22789 - 6;
         graphics.method_25294(x, top, x + 3, bottom, 0x40FFFFFF);
         graphics.method_25294(x, thumbTop, x + 3, thumbTop + thumb, 0xC0FFFFFF);
      }
   }

   /** Extra per-row drawing, such as a colour swatch. */
   void renderRow(class_332 graphics, String label, int y) {
   }

   @Override
   public void method_25419() {
      VisualsConfig.save();
      this.field_22787.method_1507(this.parent);
   }

   @Override
   public boolean method_25421() {
      return false;
   }

   private record Row(String label, int y, class_339[] widgets) {
   }
}
