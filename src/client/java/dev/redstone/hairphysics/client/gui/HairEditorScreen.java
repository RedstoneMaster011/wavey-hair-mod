package dev.redstone.hairphysics.client.gui;

import dev.redstone.hairphysics.client.HairphysicsClient;
import dev.redstone.hairphysics.client.data.HairDefinition;
import dev.redstone.hairphysics.client.data.HairStrand;
import dev.redstone.hairphysics.client.data.SkinRegion;
import dev.redstone.hairphysics.client.data.SkinMetadataLoader;
import dev.redstone.hairphysics.client.data.SkinUvMapper;
import dev.redstone.hairphysics.client.physics.PhysicsTickHandler;
import dev.redstone.hairphysics.client.render.MaskedSkinTextureManager;
import dev.redstone.hairphysics.client.render.SkinTextureCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;











public class HairEditorScreen extends Screen {

    
    private static final int SCALE   = 6;
    private static final int SKIN_S  = 64 * SCALE;
    private static final int PAD     = 8;
    private static final int LIST_W  = 144;
    private static final int PROPS_W = 190;
    private static final int ROW_H   = 20;
    private static final int BOT_H   = 30;
    private static final int DROP_H  = 18;
    private static final int DROP_ITEM_H = 18;
    private static final int[][] SKIN_LAYER_MAP = {
        {0, 0, 32, 16, 32, 0},    
        {16, 16, 24, 16, 0, 16},  
        {40, 16, 16, 16, 0, 16},  
        {0, 16, 16, 16, 0, 16},   
        {32, 48, 16, 16, 16, 0},  
        {16, 48, 16, 16, -16, 0}, 
    };
    private static final Pattern ORIGIN_ID = Pattern.compile("^origin_(\\d+)$");
    private static final Pattern ATTACHED_HAIR_ID = Pattern.compile("^(.+)_hair_(\\d+)$");

    
    private static final String[] S_LABELS = {
        "Attach X (side)","Attach Y (up)","Attach Z (front)",
        "Chain Segments","Length Scale","Stiffness",
        "Gravity","Damping","Move Response","Wind Strength",
        "Wind Direction","Hair Thickness","Rotation Amount"
    };
    private static final String[] S_HELP = {
        "Moves the root left/right on the selected bone.",
        "Moves the root up/down on the selected bone.",
        "Moves the root front/back on the selected bone.",
        "More segments bend smoother.",
        "Multiplies the selected skin patch height.",
        "Higher keeps the hair straighter.",
        "Pulls the hair downward.",
        "Higher smooths/suppresses velocity.",
        "How much player movement pushes the hair.",
        "How strongly ambient wind sways the hair.",
        "-1 blows left, 1 blows right.",
        "Scales the rendered hair sheet width.",
        "How strongly the sheet rotates/sways."
    };
    private static final float[] S_MIN = {-0.5f,-0.5f,-0.5f, 2, 0.25f, 0,    0,    0,   0,   0, -1.0f, 0.01f, 0.0f};
    private static final float[] S_MAX = { 0.5f, 0.5f, 0.5f,16, 2.00f, 1.0f, 0.2f, 1.f, 2.f, 2.f,  1.0f, 0.30f, 1.5f};

    private static final int TEXT_WHITE  = 0xFFFFFFFF;
    private static final int TEXT_MUTED  = 0xFFCCCCCC;
    private static final int TEXT_DIM    = 0xFF888888;
    private static final int TEXT_STATUS = 0xFFAAFFAA;

    
    private final List<OriginEntry> origins = new ArrayList<>();
    private final List<File> presetFiles = new ArrayList<>();
    private int sel = -1;
    private Identifier skinId;
    private UUID playerUuid;
    private boolean slim;
    private String selectedPresetName = "";
    private String attachOriginId = "";
    private boolean presetDropdownOpen;
    private boolean originDropdownOpen;
    private int listScrollRows;

    
    private double mouseX, mouseY;

    
    private boolean skinDragging;
    private int dragU0, dragV0;

    
    private int dragSliderIdx = -1;
    private long lastLiveSliderUpdateMs;

    
    private int skinX, skinY, listX, listY, propsX, propsY, botY;

    
    private TextFieldWidget idField, boneField, presetNameField;

    
    private final float[] sv = new float[S_LABELS.length];

    private String status = "Add an origin, attach hair to it, then drag on the skin to set the hair region.";

    

    public HairEditorScreen() {
        super(Text.literal("Hair Physics Editor"));
    }

    

    @Override
    protected void init() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            playerUuid = mc.player.getUuid();
        }
        refreshActiveSkin();
        reloadPresetList();

        skinX  = PAD;
        skinY  = 14;
        listX  = skinX + SKIN_S + PAD;
        listY  = skinY;
        propsX = listX + LIST_W + PAD;
        propsY = skinY;
        botY   = this.height - BOT_H;

        addDrawableChild(ButtonWidget.builder(Text.literal("+ Origin"), btn -> addOrigin())
            .dimensions(listX, listY, (LIST_W - 2) / 2, ROW_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), btn -> deleteSelected())
            .dimensions(listX + (LIST_W - 2) / 2 + 2, listY, (LIST_W - 2) / 2, ROW_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Attach Hair"), btn -> attachHairToSelected())
            .dimensions(listX, listY + ROW_H + 2, 74, ROW_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Style ↔"), btn -> toggleStyle())
            .dimensions(propsX, botY - 48, PROPS_W / 2 - 1, ROW_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Layer ↔"), btn -> toggleLayer())
            .dimensions(propsX + PROPS_W / 2 + 1, botY - 48, PROPS_W / 2 - 1, ROW_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Export Preset"), btn -> doExportPreset())
            .dimensions(PAD, botY + 5, 106, ROW_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Load Preset"), btn -> doLoadPreset())
            .dimensions(PAD + 110, botY + 5, 82, ROW_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Del Preset"), btn -> doDeletePreset())
            .dimensions(PAD + 196, botY + 5, 82, ROW_H).build());

        presetNameField = new TextFieldWidget(textRenderer, PAD, botY - 20, 110, 16,
            Text.literal("Preset Name"));
        presetNameField.setMaxLength(64);
        presetNameField.setText(selectedPresetName.isBlank() ? "hair" : selectedPresetName);
        addDrawableChild(presetNameField);

        idField = new TextFieldWidget(textRenderer, propsX, propsY + 10, PROPS_W, 16,
            Text.literal("Origin ID"));
        idField.setMaxLength(64);
        idField.setChangedListener(s -> {
            if (sel < 0) return;
            OriginEntry origin = origins.get(sel);
            String oldId = origin.id;
            origin.id = s;
            if (origin.anchorOnly) {
                renameAttachedHair(oldId, s);
            }
        });
        addDrawableChild(idField);

        boneField = new TextFieldWidget(textRenderer, propsX, propsY + 38, PROPS_W, 16,
            Text.literal("Bone"));
        boneField.setMaxLength(32);
        boneField.setChangedListener(s -> {
            if (sel < 0) return;
            OriginEntry origin = origins.get(sel);
            origin.bone = s;
            if (origin.anchorOnly) {
                syncAttachedOrigins(origin);
            }
        });
        addDrawableChild(boneField);

        refreshFields();
    }

    

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        refreshActiveSkin();

        ctx.fill(0, 0, this.width, this.height, 0xBB000000);
        ctx.drawTextWithShadow(textRenderer, "§bHair Physics Editor  §7[G / ESC to close]", PAD, 3, TEXT_WHITE);

        drawSkinCanvas(ctx);
        drawOriginList(ctx, mx, my);
        drawPropsPanel(ctx);
        drawSliders(ctx, mx, my);
        drawBottomBar(ctx);

        super.render(ctx, mx, my, delta);
        drawDropdownPopups(ctx, mx, my);
    }

    private void drawSkinCanvas(DrawContext ctx) {
        ctx.fill(skinX - 1, skinY - 1, skinX + SKIN_S + 1, skinY + SKIN_S + 1, 0xFF222222);
        if (skinId != null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED,
                skinId, skinX, skinY, 0f, 0f, SKIN_S, SKIN_S, 64, 64, 64, 64);
        }
        for (int g = 0; g <= 64; g += 8) {
            ctx.fill(skinX + g * SCALE, skinY, skinX + g * SCALE + 1, skinY + SKIN_S, 0x30FFFFFF);
            ctx.fill(skinX, skinY + g * SCALE, skinX + SKIN_S, skinY + g * SCALE + 1, 0x30FFFFFF);
        }
        for (int i = 0; i < origins.size(); i++) {
            OriginEntry o = origins.get(i);
            if (o.anchorOnly) continue;
            int rx = skinX + o.regionU * SCALE, ry = skinY + o.regionV * SCALE;
            int rw = o.regionW * SCALE,         rh = o.regionH * SCALE;
            ctx.fill(rx, ry, rx + rw, ry + rh, o.color);
            if (i == sel) {
                ctx.fill(rx,      ry,      rx+rw,   ry+1,    0xFFFFFFFF);
                ctx.fill(rx,      ry+rh-1, rx+rw,   ry+rh,   0xFFFFFFFF);
                ctx.fill(rx,      ry,      rx+1,    ry+rh,   0xFFFFFFFF);
                ctx.fill(rx+rw-1, ry,      rx+rw,   ry+rh,   0xFFFFFFFF);
            }
        }
        for (int i = 0; i < origins.size(); i++) {
            OriginEntry o = origins.get(i);
            if (o.anchorOnly) {
                drawOriginMarker(ctx, o, i == sel);
            }
        }
        ctx.fill(skinX-1, skinY-1, skinX+SKIN_S+1, skinY,             0xFF888888);
        ctx.fill(skinX-1, skinY+SKIN_S, skinX+SKIN_S+1, skinY+SKIN_S+1, 0xFF888888);
        ctx.fill(skinX-1, skinY, skinX, skinY+SKIN_S,                  0xFF888888);
        ctx.fill(skinX+SKIN_S, skinY, skinX+SKIN_S+1, skinY+SKIN_S,    0xFF888888);
    }

    private void drawOriginMarker(DrawContext ctx, OriginEntry origin, boolean selected) {
        int cx = skinX + origin.regionU * SCALE + SCALE / 2;
        int cy = skinY + origin.regionV * SCALE + SCALE / 2;
        int color = selected ? 0xFFFFFFFF : 0xFFBBBBBB;
        ctx.fill(cx - 5, cy - 1, cx + 6, cy + 2, 0xFF000000);
        ctx.fill(cx - 1, cy - 5, cx + 2, cy + 6, 0xFF000000);
        ctx.fill(cx - 4, cy, cx + 5, cy + 1, color);
        ctx.fill(cx, cy - 4, cx + 1, cy + 5, color);
    }

    private void drawOriginList(DrawContext ctx, int mx, int my) {
        drawOriginTargetDropdown(ctx, mx, my);

        int startY = hairListTop();
        ctx.drawTextWithShadow(textRenderer, "§7Origins / Hair:", listX, startY, TEXT_WHITE);
        int rowsTop = originListRowsTop();
        int rowsBottom = originListRowsBottom();
        clampListScroll();

        ctx.enableScissor(listX, rowsTop, listX + LIST_W, rowsBottom);
        int visibleRows = visibleOriginRows();
        int end = Math.min(origins.size(), listScrollRows + visibleRows + 1);
        for (int i = listScrollRows; i < end; i++) {
            OriginEntry o = origins.get(i);
            int iy = rowsTop + (i - listScrollRows) * 22;
            boolean hov = mx >= listX && mx < listX + LIST_W && my >= iy && my < iy + 20;
            ctx.fill(listX, iy, listX + LIST_W, iy + 20,
                i == sel ? 0xFF3A3A5C : hov ? 0xFF303030 : 0xFF1E1E1E);
            if (o.anchorOnly) {
                ctx.drawTextWithShadow(textRenderer, "+", listX + 6, iy + 5, TEXT_MUTED);
                ctx.drawTextWithShadow(textRenderer, displayName(o), listX + 18, iy + 2, TEXT_WHITE);
                ctx.drawTextWithShadow(textRenderer, "origin point " + o.bone, listX + 18, iy + 11, TEXT_MUTED);
            } else {
                ctx.fill(listX + 3, iy + 4, listX + 14, iy + 15, o.solidColor());
                ctx.drawTextWithShadow(textRenderer, displayName(o), listX + 18, iy + 2, TEXT_WHITE);
                ctx.drawTextWithShadow(textRenderer,
                    o.regionW + "x" + o.regionH + " " + o.layer + " " + o.bone,
                    listX + 18, iy + 11, TEXT_MUTED);
            }
        }
        ctx.disableScissor();
        drawOriginListScrollbar(ctx);
    }

    private void drawPropsPanel(DrawContext ctx) {
        ctx.drawTextWithShadow(textRenderer, "§eID:", propsX, propsY, TEXT_WHITE);
        ctx.drawTextWithShadow(textRenderer, "§eBone:", propsX, propsY + 28, TEXT_WHITE);
        if (sel >= 0) {
            OriginEntry o = origins.get(sel);
            int y = propsY + 58;
            if (o.anchorOnly) {
                ctx.drawTextWithShadow(textRenderer, "§eOrigin Point:", propsX, y, TEXT_WHITE);
                ctx.drawTextWithShadow(textRenderer,
                    "§f" + o.regionU + "," + o.regionV + "  §7click skin to move",
                    propsX, y + 10, TEXT_MUTED);
                ctx.drawTextWithShadow(textRenderer, "§7Attach Hair makes colored hair pieces.", propsX, y + 20, TEXT_DIM);
            } else {
                ctx.drawTextWithShadow(textRenderer, "§eSkin Region:", propsX, y, TEXT_WHITE);
                ctx.drawTextWithShadow(textRenderer,
                    "§f" + o.regionU + "," + o.regionV + "  " + o.regionW + "×" + o.regionH,
                    propsX, y + 10, TEXT_MUTED);
                ctx.drawTextWithShadow(textRenderer, "§7← drag on skin canvas", propsX, y + 20, TEXT_DIM);
                ctx.drawTextWithShadow(textRenderer,
                    "§eStyle: §f" + o.style + "   §eLayer: §f" + o.layer,
                    propsX, botY - 52, TEXT_WHITE);
            }
        } else {
            ctx.drawTextWithShadow(textRenderer, "§7Select an origin to edit",
                propsX, propsY + 70, TEXT_DIM);
        }
    }

    private int sliderAreaTop() { return propsY + 96; }
    private static final int KNOB_H   = 10;
    private static final int SLIDE_ROW_H = 23;

    private void drawSliders(DrawContext ctx, int mx, int my) {
        if (sel < 0) return;
        if (dragSliderIdx < 0) {
            syncSV(origins.get(sel));
        }
        ctx.drawTextWithShadow(textRenderer, "§6Physics & Transform:", propsX, sliderAreaTop() - 12, TEXT_WHITE);
        int y = sliderAreaTop();
        for (int i = 0; i < S_LABELS.length; i++) {
            String val = (i == 3) ? String.valueOf((int) sv[i]) : String.format("%.3f", sv[i]);
            ctx.drawTextWithShadow(textRenderer, "§7" + S_LABELS[i] + " §f" + val, propsX, y, TEXT_WHITE);
            y += 11;
            ctx.fill(propsX, y + 3, propsX + PROPS_W, y + 7, 0xFF444444);
            float t = clamp01((sv[i] - S_MIN[i]) / (S_MAX[i] - S_MIN[i]));
            int fw = (int)(t * PROPS_W);
            ctx.fill(propsX, y + 3, propsX + fw, y + 7, 0xFF3399FF);
            int kx = propsX + fw - KNOB_H / 2;
            boolean kHov = mx >= kx && mx < kx + KNOB_H && my >= y && my < y + KNOB_H;
            ctx.fill(kx, y, kx + KNOB_H, y + KNOB_H, kHov ? 0xFFFFFFFF : 0xFFCCCCCC);
            y += KNOB_H + 2;
        }
        int helpIdx = dragSliderIdx >= 0 ? dragSliderIdx : hitSlider(mx, my);
        if (helpIdx >= 0) {
            ctx.drawTextWithShadow(textRenderer, "§8" + S_HELP[helpIdx], propsX, botY - 72, TEXT_MUTED);
        }
    }

    private void drawBottomBar(DrawContext ctx) {
        ctx.fill(0, botY, this.width, this.height, 0xFF111111);
        ctx.fill(0, botY, this.width, botY + 1, 0xFF555555);
        drawPresetDropdown(ctx, (int) mouseX, (int) mouseY);
        ctx.drawTextWithShadow(textRenderer, status, PAD + 294, botY + 10, TEXT_STATUS);
    }

    private void drawOriginTargetDropdown(DrawContext ctx, int mx, int my) {
        int x = originDropdownX(), y = originDropdownY(), w = originDropdownW();
        String label = attachOriginId == null || attachOriginId.isBlank() ? "target origin" : attachOriginId;
        drawDropdownButton(ctx, x, y, w, label, originDropdownOpen, mx, my);
    }

    private void drawPresetDropdown(DrawContext ctx, int mx, int my) {
        int x = presetDropdownX(), y = presetDropdownY(), w = presetDropdownW();
        String label = selectedPresetName == null || selectedPresetName.isBlank() ? "select preset" : selectedPresetName;
        drawDropdownButton(ctx, x, y, w, label, presetDropdownOpen, mx, my);
    }

    private void drawDropdownPopups(DrawContext ctx, int mx, int my) {
        if (originDropdownOpen) {
            List<OriginEntry> anchors = anchorOrigins();
            int x = originDropdownX(), y = originDropdownY() + DROP_H, w = originDropdownW();
            int count = Math.min(anchors.size(), maxOriginOptions());
            for (int i = 0; i < count; i++) {
                OriginEntry anchor = anchors.get(i);
                drawDropdownOption(ctx, x, y + i * DROP_ITEM_H, w, anchor.id, mx, my);
            }
        }

        if (presetDropdownOpen) {
            int x = presetDropdownX(), y = presetOptionsTop(), w = presetDropdownW();
            int count = maxPresetOptions();
            for (int i = 0; i < count; i++) {
                String name = JsonExporter.presetNameFromFile(presetFiles.get(i));
                drawDropdownOption(ctx, x, y + i * DROP_ITEM_H, w, name, mx, my);
            }
        }
    }

    private void drawDropdownButton(DrawContext ctx, int x, int y, int w, String label,
                                    boolean open, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, DROP_H);
        ctx.fill(x, y, x + w, y + DROP_H, hover ? 0xFF3A3A5C : 0xFF1E1E1E);
        ctx.fill(x, y, x + w, y + 1, 0xFF666666);
        ctx.fill(x, y + DROP_H - 1, x + w, y + DROP_H, 0xFF111111);
        ctx.drawTextWithShadow(textRenderer, trim(label, w - 16), x + 4, y + 5, TEXT_WHITE);
        ctx.drawTextWithShadow(textRenderer, open ? "^" : "v", x + w - 10, y + 5, TEXT_MUTED);
    }

    private void drawDropdownOption(DrawContext ctx, int x, int y, int w, String label, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, DROP_ITEM_H);
        ctx.fill(x, y, x + w, y + DROP_ITEM_H, hover ? 0xFF3A3A5C : 0xFF202020);
        ctx.fill(x, y, x + w, y + 1, 0xFF555555);
        ctx.drawTextWithShadow(textRenderer, trim(label, w - 8), x + 4, y + 5, TEXT_WHITE);
    }

    

    @Override
    public void mouseMoved(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mx = click.x(), my = click.y();
        this.mouseX = mx;
        this.mouseY = my;

        if (handleDropdownClick((int) mx, (int) my)) {
            return true;
        }

        
        if (mx >= skinX && mx < skinX + SKIN_S && my >= skinY && my < skinY + SKIN_S) {
            if (sel >= 0) {
                dragU0 = clampSkin((int)((mx - skinX) / SCALE));
                dragV0 = clampSkin((int)((my - skinY) / SCALE));
                skinDragging = true;
                OriginEntry o = origins.get(sel);
                if (o.anchorOnly) {
                    placeOriginMarker(o, dragU0, dragV0);
                    applyInferredBoneFromPoint(o, dragU0, dragV0);
                    return true;
                }
                o.regionU = dragU0; o.regionV = dragV0; o.regionW = 1; o.regionH = 1;
                applyInferredBoneFromRegion(o);
                return true;
            }
            status = "§cSelect an origin first.";
            return true;
        }

        
        int rowsTop = originListRowsTop();
        int rowsBottom = originListRowsBottom();
        if (mx >= listX && mx < listX + LIST_W && my >= rowsTop && my < rowsBottom) {
            int idx = listScrollRows + (int)((my - rowsTop) / 22);
            int iy = rowsTop + (idx - listScrollRows) * 22;
            if (idx >= 0 && idx < origins.size() && my >= iy && my < iy + 20) {
                selectOrigin(idx);
                return true;
            }
        }

        
        int si = hitSlider((int) mx, (int) my);
        if (si >= 0) {
            dragSliderIdx = si;
            applySliderValue(si, (int) mx);
            saveLiveHairThrottled(true);
            return true;
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double mx = click.x(), my = click.y();
        this.mouseX = mx;
        this.mouseY = my;

        if (skinDragging && sel >= 0) {
            int u = clampSkin((int)((mx - skinX) / SCALE));
            int v = clampSkin((int)((my - skinY) / SCALE));
            OriginEntry o = origins.get(sel);
            if (o.anchorOnly) {
                placeOriginMarker(o, u, v);
                applyInferredBoneFromPoint(o, u, v);
                return true;
            }
            o.regionU = Math.min(dragU0, u); o.regionV = Math.min(dragV0, v);
            o.regionW = Math.max(1, Math.abs(u - dragU0) + 1);
            o.regionH = Math.max(1, Math.abs(v - dragV0) + 1);
            applyInferredBoneFromRegion(o);
            return true;
        }
        if (dragSliderIdx >= 0) {
            applySliderValue(dragSliderIdx, (int) mx);
            saveLiveHairThrottled(false);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (inside((int) mouseX, (int) mouseY, listX, originListRowsTop(),
            LIST_W, originListRowsBottom() - originListRowsTop()) && maxListScrollRows() > 0) {
            listScrollRows += verticalAmount > 0.0 ? -1 : 1;
            clampListScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.mouseX = click.x();
        this.mouseY = click.y();
        boolean changed = skinDragging || dragSliderIdx >= 0;
        if (skinDragging) {
            skinDragging = false;
            if (sel >= 0) {
                OriginEntry o = origins.get(sel);
                if (o.anchorOnly) {
                    status = "Origin point: " + o.regionU + "," + o.regionV;
                } else {
                    status = "Region: " + o.regionU + "," + o.regionV + " " + o.regionW + "×" + o.regionH;
                }
            }
        }
        dragSliderIdx = -1;
        if (changed) {
            saveLiveHair("Updated live hair.");
        }
        return super.mouseReleased(click);
    }

    

    private int hitSlider(int mx, int my) {
        if (sel < 0) return -1;
        int y = sliderAreaTop();
        for (int i = 0; i < S_LABELS.length; i++) {
            int trackY = y + 11;
            if (mx >= propsX && mx <= propsX + PROPS_W && my >= trackY && my < trackY + KNOB_H) return i;
            y += SLIDE_ROW_H;
        }
        return -1;
    }

    private float sliderValFromX(int si, int mx) {
        return S_MIN[si] + clamp01((float)(mx - propsX) / PROPS_W) * (S_MAX[si] - S_MIN[si]);
    }

    private void syncSV(OriginEntry o) {
        OriginEntry placement = placementSliderTarget(o);
        OriginEntry global = globalSliderSource();
        sv[0]=placement.offsetX; sv[1]=placement.offsetY; sv[2]=placement.offsetZ; sv[3]=global.segments;
        sv[4]=global.segmentLength; sv[5]=global.stiffness; sv[6]=global.gravity;
        sv[7]=global.damping; sv[8]=global.moveResponse; sv[9]=global.windResponse;
        sv[10]=global.windDirection; sv[11]=global.thickness;
        sv[12]=global.motionIntensity;
    }

    private void applyToOrigin(OriginEntry o) {
        o.offsetX=sv[0]; o.offsetY=sv[1]; o.offsetZ=sv[2];
        o.segments=Math.max(2,(int)sv[3]); o.segmentLength=sv[4]; o.stiffness=sv[5];
        o.gravity=sv[6]; o.damping=sv[7]; o.moveResponse=sv[8]; o.windResponse=sv[9];
        o.windDirection=sv[10]; o.thickness=sv[11];
        o.motionIntensity=sv[12];
        if (o.anchorOnly) {
            syncAttachedOrigins(o);
        }
    }

    private void applySliderValue(int si, int mx) {
        sv[si] = sliderValFromX(si, mx);
        if (isGlobalSlider(si)) {
            applyGlobalSlider(si);
            return;
        }

        OriginEntry target = placementSliderTarget(sel >= 0 ? origins.get(sel) : null);
        if (target != null) {
            applyPlacementSliders(target);
        }
    }

    private void applyPlacementSliders(OriginEntry origin) {
        origin.offsetX = sv[0];
        origin.offsetY = sv[1];
        origin.offsetZ = sv[2];
        if (origin.anchorOnly) {
            syncAttachedOrigins(origin);
        }
    }

    private void applyGlobalSlider(int si) {
        for (OriginEntry origin : origins) {
            switch (si) {
                case 3 -> origin.segments = Math.max(2, (int) sv[3]);
                case 4 -> origin.segmentLength = sv[4];
                case 5 -> origin.stiffness = sv[5];
                case 6 -> origin.gravity = sv[6];
                case 7 -> origin.damping = sv[7];
                case 8 -> origin.moveResponse = sv[8];
                case 9 -> origin.windResponse = sv[9];
                case 10 -> origin.windDirection = sv[10];
                case 11 -> origin.thickness = sv[11];
                case 12 -> origin.motionIntensity = sv[12];
                default -> {}
            }
        }
    }

    private static boolean isGlobalSlider(int si) {
        return si >= 3;
    }

    private OriginEntry placementSliderTarget(OriginEntry origin) {
        if (origin == null) return null;
        OriginEntry anchor = findAnchorFor(origin);
        return anchor == null ? origin : anchor;
    }

    private OriginEntry globalSliderSource() {
        for (OriginEntry origin : origins) {
            if (!origin.anchorOnly) {
                return origin;
            }
        }
        return sel >= 0 && sel < origins.size() ? origins.get(sel) : new OriginEntry("defaults", true);
    }

    private void saveLiveHairThrottled(boolean force) {
        long now = System.currentTimeMillis();
        if (force || now - lastLiveSliderUpdateMs >= 120L) {
            lastLiveSliderUpdateMs = now;
            saveLiveHair("Updated live hair.");
        }
    }

    

    private void addOrigin() {
        String id = nextOriginId();
        origins.add(new OriginEntry(id, true));
        attachOriginId = id;
        selectOrigin(origins.size() - 1);
        status = "Added origin \"" + id + "\". Click the skin to place its marker.";
    }

    private void attachHairToSelected() {
        OriginEntry anchor = selectedAttachOrigin();
        if (anchor == null) {
            status = "§cSelect a hair origin first.";
            return;
        }

        OriginEntry source = sel >= 0 ? origins.get(sel) : anchor;
        String parentId = anchor.id;
        OriginEntry attached = new OriginEntry(nextHairId(parentId), false);

        copyLinkedSettings(attached, anchor);

        attached.regionU = source.regionU;
        attached.regionV = source.regionV;
        attached.regionW = source.anchorOnly ? 8 : source.regionW;
        attached.regionH = source.anchorOnly ? 8 : source.regionH;
        attached.layer = source.layer;
        SkinUvMapper.SurfacePoint attachedPoint = surfacePoint(attached);
        if (shouldUseSurfacePoint(anchor, attachedPoint)) {
            applySurfacePoint(anchor, attachedPoint);
            syncAttachedOrigins(anchor);
            copyLinkedSettings(attached, anchor);
        }

        if (!source.anchorOnly) {
            attached.style = source.style;
        }

        origins.add(attached);
        selectOrigin(origins.size() - 1);
        status = "Attached \"" + attached.id + "\" to " + anchor.id + ".";
    }

    private void deleteSelected() {
        if (sel < 0 || origins.isEmpty()) return;
        String id = origins.get(sel).id;
        origins.remove(sel);
        sel = origins.isEmpty() ? -1 : Math.min(sel, origins.size() - 1);
        clampListScroll();
        ensureSelectionVisible();
        if (id.equals(attachOriginId) || selectedAttachOrigin() == null) {
            attachOriginId = firstAnchorId();
        }
        refreshFields();
        status = "Deleted \"" + id + "\".";
    }

    private void selectOrigin(int idx) {
        sel = idx;
        ensureSelectionVisible();
        refreshFields();
        if (idx >= 0) {
            OriginEntry origin = origins.get(idx);
            if (origin.anchorOnly) {
                attachOriginId = origin.id;
            }
            status = "Selected: " + origin.id;
        }
    }

    private void refreshFields() {
        if (idField == null || boneField == null) return;
        if (sel >= 0) {
            OriginEntry o = origins.get(sel);
            idField.setText(o.id); boneField.setText(o.bone);
            idField.setEditable(true); boneField.setEditable(true);
            syncSV(o);
        } else {
            idField.setText(""); boneField.setText("");
            idField.setEditable(false); boneField.setEditable(false);
        }
    }

    private void toggleStyle() {
        if (sel < 0) return;
        OriginEntry o = origins.get(sel);
        if (o.anchorOnly) {
            status = "Origin points do not have a render style.";
            return;
        }
        o.style = o.style.equals("ribbon") ? "box" : "ribbon";
        status = "Style → " + o.style;
    }

    private void toggleLayer() {
        if (sel < 0) return;
        OriginEntry o = origins.get(sel);
        if (o.anchorOnly) {
            status = "Origin points do not have a skin layer.";
            return;
        }
        String targetLayer = o.layer.equals("outer") ? "inner" : "outer";
        boolean moved = moveRegionToLayer(o, targetLayer);
        o.layer = targetLayer;
        status = "Layer → " + o.layer + (moved ? " region" : "");
    }

    private void doExportPreset() {
        if (origins.isEmpty()) { status = "§cNo origins to export!"; return; }
        if (presetNameField == null || presetNameField.getText().trim().isBlank()) {
            status = "§cType a preset name first."; return;
        }
        try {
            syncAllAttachedOrigins();
            File out = JsonExporter.exportPreset(origins, presetNameField.getText(), slim);
            reloadPresetList();
            selectedPresetName = JsonExporter.presetNameFromFile(out);
            reloadLiveHair();
            status = "§aExported → " + out.getName();
        } catch (Exception e) {
            status = "§cExport failed: " + e.getMessage();
            HairphysicsClient.LOGGER.error("[HairPhysics] Export failed", e);
        }
    }

    private void doLoadPreset() {
        try {
            reloadPresetList();
            File preset = selectedPresetFile();
            if (preset == null) {
                status = "§cNo presets saved yet."; return;
            }

            Optional<HairDefinition> defOpt = SkinMetadataLoader.loadFromFile(preset.toPath(), true);
            if (defOpt.isEmpty() || defOpt.get().strands.isEmpty()) {
                status = "§cJSON empty or unreadable."; return;
            }
            loadDefinitionIntoEditor(defOpt.get());
            selectedPresetName = JsonExporter.presetNameFromFile(preset);
            if (presetNameField != null) {
                presetNameField.setText(selectedPresetName);
            }
            JsonExporter.exportActive(origins, slim);
            selectOrigin(origins.isEmpty() ? -1 : 0);
            reloadLiveHair();
            status = "§aLoaded preset → " + preset.getName();
        } catch (Exception e) {
            status = "§cLoad failed: " + e.getMessage();
        }
    }

    private void doDeletePreset() {
        try {
            reloadPresetList();
            File preset = selectedPresetFile();
            if (preset == null) {
                status = "§cNo preset selected.";
                return;
            }

            String deletedName = preset.getName();
            if (!preset.delete()) {
                status = "§cCould not delete preset.";
                return;
            }

            presetDropdownOpen = false;
            reloadPresetList();
            if (presetNameField != null) {
                presetNameField.setText(selectedPresetName == null || selectedPresetName.isBlank()
                    ? "hair"
                    : selectedPresetName);
            }
            status = "§aDeleted preset → " + deletedName;
        } catch (Exception e) {
            status = "§cDelete failed: " + e.getMessage();
        }
    }

    

    private void reloadLiveHair() {
        if (playerUuid == null) return;

        SkinMetadataLoader.clearCache(playerUuid);
        MaskedSkinTextureManager.clear(playerUuid);
        PhysicsTickHandler.MANAGER.remove(playerUuid);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && playerUuid.equals(mc.player.getUuid())) {
            PhysicsTickHandler.MANAGER.getOrCreate(mc.player);
        }
    }

    private int hairListTop() {
        return listY + ROW_H * 2 + 6;
    }

    private int originListRowsTop() {
        return hairListTop() + 12;
    }

    private int originListRowsBottom() {
        return Math.max(originListRowsTop() + 22, botY - 4);
    }

    private int visibleOriginRows() {
        return Math.max(1, (originListRowsBottom() - originListRowsTop()) / 22);
    }

    private int maxListScrollRows() {
        return Math.max(0, origins.size() - visibleOriginRows());
    }

    private void clampListScroll() {
        listScrollRows = Math.max(0, Math.min(listScrollRows, maxListScrollRows()));
    }

    private void ensureSelectionVisible() {
        if (sel < 0) {
            clampListScroll();
            return;
        }
        int visibleRows = visibleOriginRows();
        if (sel < listScrollRows) {
            listScrollRows = sel;
        } else if (sel >= listScrollRows + visibleRows) {
            listScrollRows = sel - visibleRows + 1;
        }
        clampListScroll();
    }

    private void drawOriginListScrollbar(DrawContext ctx) {
        int maxScroll = maxListScrollRows();
        if (maxScroll <= 0) return;

        int rowsTop = originListRowsTop();
        int rowsBottom = originListRowsBottom();
        int trackH = Math.max(1, rowsBottom - rowsTop);
        int thumbH = Math.max(14, trackH * visibleOriginRows() / Math.max(1, origins.size()));
        int thumbY = rowsTop + (trackH - thumbH) * listScrollRows / maxScroll;
        int x = listX + LIST_W - 5;

        ctx.fill(x, rowsTop, x + 4, rowsBottom, 0x80000000);
        ctx.fill(x, thumbY, x + 4, thumbY + thumbH, 0xFF888888);
    }

    private boolean handleDropdownClick(int mx, int my) {
        if (presetDropdownOpen) {
            int presetIdx = hitPresetOption(mx, my);
            if (presetIdx >= 0) {
                File preset = presetFiles.get(presetIdx);
                selectedPresetName = JsonExporter.presetNameFromFile(preset);
                if (presetNameField != null) {
                    presetNameField.setText(selectedPresetName);
                }
                presetDropdownOpen = false;
                return true;
            }
            if (!inside(mx, my, presetDropdownX(), presetDropdownY(), presetDropdownW(), DROP_H)) {
                presetDropdownOpen = false;
            }
        }

        if (originDropdownOpen) {
            int originIdx = hitOriginOption(mx, my);
            if (originIdx >= 0) {
                List<OriginEntry> anchors = anchorOrigins();
                attachOriginId = anchors.get(originIdx).id;
                originDropdownOpen = false;
                return true;
            }
            if (!inside(mx, my, originDropdownX(), originDropdownY(), originDropdownW(), DROP_H)) {
                originDropdownOpen = false;
            }
        }

        if (inside(mx, my, presetDropdownX(), presetDropdownY(), presetDropdownW(), DROP_H)) {
            reloadPresetList();
            presetDropdownOpen = !presetDropdownOpen;
            originDropdownOpen = false;
            return true;
        }

        if (inside(mx, my, originDropdownX(), originDropdownY(), originDropdownW(), DROP_H)) {
            originDropdownOpen = !originDropdownOpen;
            presetDropdownOpen = false;
            if (attachOriginId == null || attachOriginId.isBlank()) {
                attachOriginId = firstAnchorId();
            }
            return true;
        }

        return false;
    }

    private int hitPresetOption(int mx, int my) {
        int count = maxPresetOptions();
        int x = presetDropdownX(), y = presetOptionsTop(), w = presetDropdownW();
        for (int i = 0; i < count; i++) {
            if (inside(mx, my, x, y + i * DROP_ITEM_H, w, DROP_ITEM_H)) {
                return i;
            }
        }
        return -1;
    }

    private int hitOriginOption(int mx, int my) {
        int count = Math.min(anchorOrigins().size(), maxOriginOptions());
        int x = originDropdownX(), y = originDropdownY() + DROP_H, w = originDropdownW();
        for (int i = 0; i < count; i++) {
            if (inside(mx, my, x, y + i * DROP_ITEM_H, w, DROP_ITEM_H)) {
                return i;
            }
        }
        return -1;
    }

    private int originDropdownX() { return listX + 78; }
    private int originDropdownY() { return listY + ROW_H + 3; }
    private int originDropdownW() { return LIST_W - 78; }
    private int presetDropdownX() { return PAD + 114; }
    private int presetDropdownY() { return botY - 20; }
    private int presetDropdownW() { return 150; }

    private int maxPresetOptions() {
        int available = Math.max(DROP_ITEM_H, presetDropdownY() - 4);
        return Math.min(presetFiles.size(), Math.max(1, available / DROP_ITEM_H));
    }

    private int presetOptionsTop() {
        return presetDropdownY() - maxPresetOptions() * DROP_ITEM_H;
    }

    private int maxOriginOptions() {
        int available = Math.max(DROP_ITEM_H, botY - (originDropdownY() + DROP_H) - 4);
        return Math.max(1, available / DROP_ITEM_H);
    }

    private List<OriginEntry> anchorOrigins() {
        List<OriginEntry> anchors = new ArrayList<>();
        for (OriginEntry origin : origins) {
            if (origin.anchorOnly) {
                anchors.add(origin);
            }
        }
        return anchors;
    }

    private OriginEntry selectedAttachOrigin() {
        if (attachOriginId != null && !attachOriginId.isBlank()) {
            int idx = findOrigin(attachOriginId);
            if (idx >= 0 && origins.get(idx).anchorOnly) {
                return origins.get(idx);
            }
        }

        if (sel >= 0) {
            OriginEntry selected = origins.get(sel);
            OriginEntry anchor = findAnchorFor(selected);
            if (anchor != null) {
                attachOriginId = anchor.id;
                return anchor;
            }
        }

        for (OriginEntry origin : origins) {
            if (origin.anchorOnly) {
                attachOriginId = origin.id;
                return origin;
            }
        }
        return null;
    }

    private String firstAnchorId() {
        for (OriginEntry origin : origins) {
            if (origin.anchorOnly) {
                return origin.id;
            }
        }
        return "";
    }

    private void reloadPresetList() {
        try {
            presetFiles.clear();
            presetFiles.addAll(JsonExporter.listPresetFiles());
            if (presetFiles.isEmpty()) {
                selectedPresetName = "";
                return;
            }

            if (selectedPresetName == null || selectedPresetName.isBlank() || selectedPresetFile() == null) {
                selectedPresetName = JsonExporter.presetNameFromFile(presetFiles.get(0));
            }
        } catch (Exception e) {
            HairphysicsClient.LOGGER.warn("[HairPhysics] Could not list presets: {}", e.getMessage());
        }
    }

    private File selectedPresetFile() {
        if (presetFiles.isEmpty()) return null;
        for (File file : presetFiles) {
            if (JsonExporter.presetNameFromFile(file).equals(selectedPresetName)) {
                return file;
            }
        }
        return selectedPresetName == null || selectedPresetName.isBlank() ? presetFiles.get(0) : null;
    }

    private void loadDefinitionIntoEditor(HairDefinition definition) {
        origins.clear();
        listScrollRows = 0;
        OriginEntry.resetColorCycle();
        for (HairStrand s : definition.strands) {
            String anchorId = parentAnchorId(s.id);
            if (!s.anchorOnly && anchorId != null && findOrigin(anchorId) < 0) {
                OriginEntry anchor = new OriginEntry(anchorId, true);
                copyFromStrand(anchor, s);
                origins.add(anchor);
            }

            OriginEntry e = new OriginEntry(s.id, s.anchorOnly);
            copyFromStrand(e, s);
            origins.add(e);
        }
        attachOriginId = firstAnchorId();
    }

    private int findOrigin(String id) {
        for (int i = 0; i < origins.size(); i++) {
            if (origins.get(i).id.equals(id)) return i;
        }
        return -1;
    }

    private static String parentAnchorId(String id) {
        if (id == null) return null;
        Matcher matcher = ATTACHED_HAIR_ID.matcher(id);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static int hairSuffix(String id) {
        if (id == null) return -1;
        Matcher matcher = ATTACHED_HAIR_ID.matcher(id);
        return matcher.matches() ? Integer.parseInt(matcher.group(2)) : -1;
    }

    private String nextOriginId() {
        Set<Integer> used = new HashSet<>();
        for (OriginEntry origin : origins) {
            Matcher matcher = ORIGIN_ID.matcher(origin.id);
            if (matcher.matches()) {
                used.add(Integer.parseInt(matcher.group(1)));
            }
        }
        int next = 1;
        while (used.contains(next)) {
            next++;
        }
        return "origin_" + next;
    }

    private String nextHairId(String parentId) {
        Set<Integer> used = new HashSet<>();
        for (OriginEntry origin : origins) {
            Matcher matcher = ATTACHED_HAIR_ID.matcher(origin.id);
            if (matcher.matches() && matcher.group(1).equals(parentId)) {
                used.add(Integer.parseInt(matcher.group(2)));
            }
        }
        int next = 1;
        while (used.contains(next)) {
            next++;
        }
        return parentId + "_hair_" + next;
    }

    private OriginEntry findAnchorFor(OriginEntry entry) {
        if (entry == null) return null;
        if (entry.anchorOnly) return entry;

        String parentId = parentAnchorId(entry.id);
        if (parentId == null) return null;

        int idx = findOrigin(parentId);
        if (idx < 0) return null;

        OriginEntry parent = origins.get(idx);
        return parent.anchorOnly ? parent : null;
    }

    private void renameAttachedHair(String oldId, String newId) {
        if (oldId == null || oldId.isBlank() || newId == null || newId.isBlank() || oldId.equals(newId)) {
            return;
        }
        for (OriginEntry origin : origins) {
            if (origin.anchorOnly) continue;
            String parentId = parentAnchorId(origin.id);
            if (oldId.equals(parentId)) {
                int suffix = hairSuffix(origin.id);
                origin.id = newId + "_hair_" + Math.max(1, suffix);
            }
        }
    }

    private void syncAllAttachedOrigins() {
        for (OriginEntry origin : origins) {
            if (origin.anchorOnly) {
                syncAttachedOrigins(origin);
            }
        }
    }

    private void syncAttachedOrigins(OriginEntry anchor) {
        if (anchor == null || anchor.id == null || anchor.id.isBlank()) return;
        for (OriginEntry origin : origins) {
            if (!origin.anchorOnly && anchor.id.equals(parentAnchorId(origin.id))) {
                copyLinkedSettings(origin, anchor);
            }
        }
    }

    private static void copyOriginSettings(OriginEntry target, OriginEntry source) {
        target.bone = source.bone;
        target.offsetX = source.offsetX;
        target.offsetY = source.offsetY;
        target.offsetZ = source.offsetZ;
    }

    private static void copyLinkedSettings(OriginEntry target, OriginEntry source) {
        copyOriginSettings(target, source);
        target.segments = source.segments;
        target.segmentLength = source.segmentLength;
        target.stiffness = source.stiffness;
        target.gravity = source.gravity;
        target.damping = source.damping;
        target.moveResponse = source.moveResponse;
        target.windResponse = source.windResponse;
        target.windDirection = source.windDirection;
        target.thickness = source.thickness;
        target.motionIntensity = source.motionIntensity;
        target.style = source.style;
    }

    private void saveLiveHair(String okStatus) {
        if (origins.isEmpty()) return;
        try {
            syncAllAttachedOrigins();
            JsonExporter.exportActive(origins, slim);
            reloadLiveHair();
            status = "§a" + okStatus;
        } catch (Exception e) {
            status = "§cLive update failed: " + e.getMessage();
            HairphysicsClient.LOGGER.error("[HairPhysics] Live editor update failed", e);
        }
    }

    private static void placeOriginMarker(OriginEntry origin, int u, int v) {
        origin.regionU = u;
        origin.regionV = v;
        origin.regionW = 1;
        origin.regionH = 1;
    }

    private void applyInferredBoneFromPoint(OriginEntry origin, int u, int v) {
        applySurfacePoint(origin, SkinUvMapper.pointFor(u, v));
        if (origin.anchorOnly) {
            syncAttachedOrigins(origin);
        }
        refreshFields();
    }

    private void applyInferredBoneFromRegion(OriginEntry origin) {
        SkinUvMapper.SurfacePoint point = surfacePoint(origin);
        OriginEntry anchor = findAnchorFor(origin);
        if (anchor != null) {
            if (shouldUseSurfacePoint(anchor, point)) {
                applySurfacePoint(anchor, point);
                syncAttachedOrigins(anchor);
                refreshFields();
            }
            return;
        }

        if (shouldUseSurfacePoint(origin, point)) {
            applySurfacePoint(origin, point);
            refreshFields();
        }
    }

    private static SkinUvMapper.SurfacePoint surfacePoint(OriginEntry origin) {
        return SkinUvMapper.pointForRegion(new SkinRegion(
            origin.regionU, origin.regionV, origin.regionW, origin.regionH, origin.layer));
    }

    private static boolean shouldUseSurfacePoint(OriginEntry origin, SkinUvMapper.SurfacePoint point) {
        return isDefaultOffset(origin)
            || isNearSurfacePoint(origin, point)
            || ("head".equalsIgnoreCase(origin.bone) && !"head".equals(point.bone()));
    }

    private static void applySurfacePoint(OriginEntry origin, SkinUvMapper.SurfacePoint point) {
        origin.bone = point.bone();
        origin.layer = point.layer();
        origin.offsetX = point.offsetX();
        origin.offsetY = point.offsetY();
        origin.offsetZ = point.offsetZ();
    }

    private static boolean isDefaultOffset(OriginEntry origin) {
        return Math.abs(origin.offsetX) < 0.0001f
            && Math.abs(origin.offsetY - 0.25f) < 0.0001f
            && Math.abs(origin.offsetZ + 0.25f) < 0.0001f;
    }

    private static boolean isNearSurfacePoint(OriginEntry origin, SkinUvMapper.SurfacePoint point) {
        return point.bone().equalsIgnoreCase(origin.bone)
            && Math.abs(origin.offsetX - point.offsetX()) <= 0.04f
            && Math.abs(origin.offsetY - point.offsetY()) <= 0.04f
            && Math.abs(origin.offsetZ - point.offsetZ()) <= 0.04f;
    }

    private static void copyFromStrand(OriginEntry e, HairStrand s) {
        e.bone = s.origin.bone;
        e.offsetX = s.origin.offsetX; e.offsetY = s.origin.offsetY; e.offsetZ = s.origin.offsetZ;
        e.regionU = s.skinRegion.u;   e.regionV = s.skinRegion.v;
        e.regionW = s.skinRegion.width; e.regionH = s.skinRegion.height;
        e.layer = s.skinRegion.layer;
        e.segments = s.physics.segments; e.segmentLength = s.physics.lengthScale;
        e.stiffness = s.physics.stiffness; e.gravity = s.physics.gravity;
        e.damping = s.physics.damping; e.moveResponse = s.physics.moveResponse;
        e.windResponse = s.physics.windResponse; e.windDirection = s.physics.windDirection;
        e.thickness = s.render.thickness;
        e.motionIntensity = s.render.motionIntensity;
        e.style = s.render.style.name().toLowerCase();
    }

    private static String displayName(OriginEntry origin) {
        if (origin.id == null || origin.id.isBlank()) {
            return "(unnamed)";
        }
        return origin.id;
    }

    private String trim(String text, int maxWidth) {
        return textRenderer.trimToWidth(text == null ? "" : text, Math.max(1, maxWidth));
    }

    private void refreshActiveSkin() {
        MinecraftClient mc = MinecraftClient.getInstance();
        UUID resolvedUuid = playerUuid;
        Identifier resolvedSkin = null;
        boolean resolvedSlim = slim;

        if (mc.player != null) {
            resolvedUuid = mc.player.getUuid();
            SkinTextures playerSkin = mc.player.getSkin();
            if (playerSkin != null) {
                resolvedSkin = playerSkin.body().texturePath();
                resolvedSlim = playerSkin.model() == PlayerSkinType.SLIM;
            }
        }

        if (resolvedSkin == null && mc.getGameProfile() != null) {
            SkinTextures profileSkin = mc.getSkinProvider()
                .supplySkinTextures(mc.getGameProfile(), true)
                .get();
            if (profileSkin != null) {
                resolvedSkin = profileSkin.body().texturePath();
                resolvedSlim = profileSkin.model() == PlayerSkinType.SLIM;
            }
        }

        if (resolvedSkin == null && resolvedUuid != null) {
            resolvedSkin = SkinTextureCache.getLastSeenSkin(resolvedUuid);
            resolvedSlim = SkinTextureCache.isSlim(resolvedUuid);
        }

        if (resolvedSkin == null) {
            SkinTextures fallbackSkin = mc.getGameProfile() != null
                ? DefaultSkinHelper.getSkinTextures(mc.getGameProfile())
                : DefaultSkinHelper.getSteve();
            resolvedSkin = fallbackSkin.body().texturePath();
            resolvedSlim = fallbackSkin.model() == PlayerSkinType.SLIM;
        }

        playerUuid = resolvedUuid;
        skinId = resolvedSkin;
        slim = resolvedSlim;
        if (playerUuid != null && skinId != null) {
            SkinTextureCache.store(playerUuid, skinId, slim);
        }
    }

    private static int clampSkin(int v) { return Math.max(0, Math.min(63, v)); }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean moveRegionToLayer(OriginEntry origin, String targetLayer) {
        boolean toOuter = "outer".equalsIgnoreCase(targetLayer);
        for (int[] map : SKIN_LAYER_MAP) {
            int innerX = map[0], innerY = map[1], width = map[2], height = map[3];
            int dx = map[4], dy = map[5];
            int sourceX = toOuter ? innerX : innerX + dx;
            int sourceY = toOuter ? innerY : innerY + dy;

            if (containsRegion(origin, sourceX, sourceY, width, height)) {
                origin.regionU += toOuter ? dx : -dx;
                origin.regionV += toOuter ? dy : -dy;
                return true;
            }
        }
        return false;
    }

    private static boolean containsRegion(OriginEntry origin, int x, int y, int width, int height) {
        return origin.regionU >= x
            && origin.regionV >= y
            && origin.regionU + origin.regionW <= x + width
            && origin.regionV + origin.regionH <= y + height;
    }

    @Override public boolean shouldPause()      { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }
}
