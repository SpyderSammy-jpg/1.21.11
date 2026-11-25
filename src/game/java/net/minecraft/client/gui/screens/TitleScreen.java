
package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TitleScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("narrator.screen.title");
    private static final Component COPYRIGHT_TEXT = Component.translatable("title.credits");
    private static final String DEMO_LEVEL_ID = "Demo_World";

    private @Nullable SplashRenderer splash;
    private @Nullable RealmsNotificationsScreen realmsNotificationsScreen;
    private boolean fading;
    private long fadeInStart;
    private final LogoRenderer logoRenderer;

    public TitleScreen() {
        this(false);
    }

    public TitleScreen(boolean p_96733_) {
        this(p_96733_, null);
    }

    public TitleScreen(boolean p_265779_, @Nullable LogoRenderer p_265067_) {
        super(TITLE);
        this.fading = p_265779_;
        this.logoRenderer = Objects.requireNonNullElseGet(p_265067_, () -> new LogoRenderer(false));
    }

    private boolean realmsNotificationsEnabled() {
        return this.realmsNotificationsScreen != null;
    }

    @Override
    public void tick() {
        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.tick();
        }
    }

    public static void registerTextures(TextureManager p_389575_) {
        p_389575_.registerForNextReload(LogoRenderer.MINECRAFT_LOGO);
        p_389575_.registerForNextReload(LogoRenderer.MINECRAFT_EDITION);
        p_389575_.registerForNextReload(PanoramaRenderer.PANORAMA_OVERLAY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        if (this.splash == null) {
            this.splash = this.minecraft.getSplashManager().getSplash();
        }

        int i = this.font.width(COPYRIGHT_TEXT);
        int j = this.width - i - 2;
        int l = this.height / 4 + 48;

        if (this.minecraft.isDemo()) {
            l = this.createDemoMenuOptions(l, 24);
        } else {
            l = this.createNormalMenuOptions(l, 24);
        }

        l = this.createTestWorldButton(l, 24);

        SpriteIconButton languageButton = this.addRenderableWidget(
            CommonButtons.language(
                20,
                p_344157_ -> this.minecraft.setScreen(
                    new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())
                ),
                true
            )
        );

        int i1 = this.width / 2 - 124;
        l += 36;
        languageButton.setPosition(i1, l);

        this.addRenderableWidget(
            Button.builder(
                Component.translatable("menu.options"),
                p_344156_ -> this.minecraft.setScreen(
                    new OptionsScreen(this, this.minecraft.options)
                )
            )
            .bounds(this.width / 2 - 100, l, 200, 20)
            .build()
        );

        SpriteIconButton accessibilityButton = this.addRenderableWidget(
            CommonButtons.accessibility(
                20,
                p_344158_ -> this.minecraft.setScreen(
                    new AccessibilityOptionsScreen(this, this.minecraft.options)
                ),
                true
            )
        );

        accessibilityButton.setPosition(this.width / 2 + 104, l);

        this.addRenderableWidget(
            new PlainTextButton(
                j,
                this.height - 10,
                i,
                10,
                COPYRIGHT_TEXT,
                p_280834_ -> this.minecraft.setScreen(
                    new CreditsAndAttributionScreen(this)
                ),
                this.font
            )
        );

        if (this.realmsNotificationsScreen == null) {
            this.realmsNotificationsScreen = new RealmsNotificationsScreen();
        }

        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.init(this.width, this.height);
        }
    }

    private int createTestWorldButton(int p_373114_, int p_372852_) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            this.addRenderableWidget(
                Button.builder(
                    Component.literal("Create Test World"),
                    p_438731_ -> CreateWorldScreen.testWorld(
                        this.minecraft,
                        () -> this.minecraft.setScreen(this)
                    )
                )
                .bounds(
                    this.width / 2 - 100,
                    p_373114_ += p_372852_,
                    200,
                    20
                )
                .build()
            );
        }

        return p_373114_;
    }

    private int createNormalMenuOptions(int p_96764_, int p_96765_) {
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("menu.singleplayer"),
                p_280832_ -> this.minecraft.setScreen(new SelectWorldScreen(this))
            )
            .bounds(this.width / 2 - 100, p_96764_, 200, 20)
            .build()
        );

        Component component = this.getMultiplayerDisabledReason();
        boolean flag = component == null;
        Tooltip tooltip = component != null ? Tooltip.create(component) : null;

        int i;

        this.addRenderableWidget(
            Button.builder(
                Component.translatable("menu.multiplayer"),
                p_280833_ -> {
                    Screen screen = this.minecraft.options.skipMultiplayerWarning
                        ? new JoinMultiplayerScreen(this)
                        : new SafetyScreen(this);

                    this.minecraft.setScreen(screen);
                }
            )
            .bounds(
                this.width / 2 - 100,
                i = p_96764_ + p_96765_,
                200,
                20
            )
            .tooltip(tooltip)
            .build()
        ).active = flag;

        return i;
    }

    private @Nullable Component getMultiplayerDisabledReason() {
        if (this.minecraft.allowsMultiplayer()) {
            return null;
        } else if (this.minecraft.isNameBanned()) {
            return Component.translatable("title.multiplayer.disabled.banned.name");
        } else {
            BanDetails bandetails = this.minecraft.multiplayerBan();

            if (bandetails != null) {
                return bandetails.expires() != null
                    ? Component.translatable("title.multiplayer.disabled.banned.temporary")
                    : Component.translatable("title.multiplayer.disabled.banned.permanent");
            } else {
                return Component.translatable("title.multiplayer.disabled");
            }
        }
    }

    private int createDemoMenuOptions(int p_96773_, int p_96774_) {
        boolean flag = this.checkDemoWorldPresence();

        this.addRenderableWidget(
            Button.builder(
                Component.translatable("menu.playdemo"),
                p_329724_ -> {
                    if (flag) {
                        this.minecraft.createWorldOpenFlows().openWorld(
                            "Demo_World",
                            () -> this.minecraft.setScreen(this)
                        );
                    } else {
                        this.minecraft.createWorldOpenFlows().createFreshLevel(
                            "Demo_World",
                            MinecraftServer.DEMO_SETTINGS,
                            WorldOptions.DEMO_OPTIONS,
                            WorldPresets::createNormalWorldDimensions,
                            this
                        );
                    }
                }
            )
            .bounds(this.width / 2 - 100, p_96773_, 200, 20)
            .build()
        );

        int i;

        Button button = this.addRenderableWidget(
            Button.builder(
                Component.translatable("menu.resetdemo"),
                p_307045_ -> {
                    LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();

                    try (
                        LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess =
                            levelstoragesource.createAccess("Demo_World")
                    ) {
                        if (levelstoragesource$levelstorageaccess.hasWorldData()) {
                            this.minecraft.setScreen(
                                new ConfirmScreen(
                                    this::confirmDemo,
                                    Component.translatable("selectWorld.deleteQuestion"),
                                    Component.translatable(
                                        "selectWorld.deleteWarning",
                                        MinecraftServer.DEMO_SETTINGS.levelName()
                                    ),
                                    Component.translatable("selectWorld.deleteButton"),
                                    CommonComponents.GUI_CANCEL
                                )
                            );
                        }
                    } catch (IOException ioexception) {
                        SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
                        LOGGER.warn("Failed to access demo world", (Throwable)ioexception);
                    }
                }
            )
            .bounds(
                this.width / 2 - 100,
                i = p_96773_ + p_96774_,
                200,
                20
            )
            .build()
        );

        button.active = flag;
        return i;
    }

    private boolean checkDemoWorldPresence() {
        try {
            boolean flag;

            try (
                LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess =
                    this.minecraft.getLevelSource().createAccess("Demo_World")
            ) {
                flag = levelstoragesource$levelstorageaccess.hasWorldData();
            }

            return flag;
        } catch (IOException ioexception) {
            SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
            LOGGER.warn("Failed to read demo world data", (Throwable)ioexception);
            return false;
        }
    }

    private void renderMagmaBackdrop(GuiGraphics graphics) {
        long time = Util.getMillis();

        // Darken the original panorama.
        graphics.fill(
            0,
            0,
            this.width,
            this.height,
            0x99080000
        );

        // Volcanic glow along the bottom.
        int glowHeight = Math.max(70, this.height / 7);

        graphics.fill(
            0,
            this.height - glowHeight,
            this.width,
            this.height,
            0x33250000
        );

        // Animated magma seams.
        for (int i = 0; i < 7; i++) {
            double wave = Math.sin((time / 850.0) + (i * 1.7));

            int x = (int)(
                ((i + 1) * (this.width / 8.0)) + wave * 24.0
            );

            int y = this.height - glowHeight + 12 + (i % 3) * 17;
            int seamWidth = 2 + (i % 3);

            graphics.fill(
                x,
                y,
                x + seamWidth,
                this.height,
                0x99FF3B00
            );

            graphics.fill(
                x - 2,
                y + 8,
                x + seamWidth + 2,
                y + 10,
                0x66FF8A00
            );
        }

        // Dark menu panel with orange borders.
        int panelLeft = this.width / 2 - 145;
        int panelTop = this.height / 4 + 24;
        int panelRight = this.width / 2 + 145;
        int panelBottom = Math.min(
            this.height - 35,
            panelTop + 220
        );

        graphics.fill(
            panelLeft,
            panelTop,
            panelRight,
            panelBottom,
            0xB0080503
        );

        graphics.fill(
            panelLeft,
            panelTop,
            panelRight,
            panelTop + 2,
            0xFFFF4A00
        );

        graphics.fill(
            panelLeft,
            panelBottom - 2,
            panelRight,
            panelBottom,
            0xFFFF4A00
        );

        graphics.fill(
            panelLeft,
            panelTop,
            panelLeft + 2,
            panelBottom,
            0xCCB52A00
        );

        graphics.fill(
            panelRight - 2,
            panelTop,
            panelRight,
            panelBottom,
            0xCCB52A00
        );
    }

    private void renderMagmaBranding(GuiGraphics graphics, float alpha) {
        String title = "MAGMA SMP";
        String subtitle = "SURVIVE THE HEAT";

        int titleX = (this.width - this.font.width(title)) / 2;
        int subtitleX = (this.width - this.font.width(subtitle)) / 2;
        int titleY = Math.max(12, this.height / 4 - 12);

        graphics.drawString(
            this.font,
            title,
            titleX + 1,
            titleY + 1,
            ARGB.white(alpha * 0.45F)
        );

        graphics.drawString(
            this.font,
            title,
            titleX,
            titleY,
            0xFFFF6A00
        );

        graphics.drawString(
            this.font,
            subtitle,
            subtitleX,
            titleY + 13,
            0xFFFFB347
        );
    }

    @Override
    public void render(
        GuiGraphics p_282860_,
        int p_281753_,
        int p_283539_,
        float p_282628_
    ) {
        if (this.fadeInStart == 0L && this.fading) {
            this.fadeInStart = Util.getMillis();
        }

        float f = 1.0F;

        if (this.fading) {
            float f1 = (float)(
                Util.getMillis() - this.fadeInStart
            ) / 2000.0F;

            if (f1 > 1.0F) {
                this.fading = false;
            } else {
                f1 = Mth.clamp(f1, 0.0F, 1.0F);
                f = Mth.clampedMap(f1, 0.5F, 1.0F, 0.0F, 1.0F);
            }

            this.fadeWidgets(f);
        }

        this.renderPanorama(p_282860_, p_282628_);
        this.renderMagmaBackdrop(p_282860_);

        super.render(
            p_282860_,
            p_281753_,
            p_283539_,
            p_282628_
        );

        this.renderMagmaBranding(p_282860_, f);

        String s = "Minecraft " + SharedConstants.getCurrentVersion().name();

        if (this.minecraft.isDemo()) {
            s = s + " Demo";
        } else {
            s = s + (
                "release".equalsIgnoreCase(this.minecraft.getVersionType())
                    ? ""
                    : "/" + this.minecraft.getVersionType()
            );
        }

        if (Minecraft.checkModStatus().shouldReportAsModified()) {
            s = s + I18n.get("menu.modded");
        }

        p_282860_.drawString(
            this.font,
            s,
            2,
            this.height - 10,
            ARGB.white(f)
        );

        if (this.realmsNotificationsEnabled() && f >= 1.0F) {
            this.realmsNotificationsScreen.render(
                p_282860_,
                p_281753_,
                p_283539_,
                p_282628_
            );
        }
    }

    @Override
    public void renderBackground(
        GuiGraphics p_294355_,
        int p_295208_,
        int p_294981_,
        float p_294740_
    ) {
    }

    @Override
    public boolean mouseClicked(
        MouseButtonEvent p_446070_,
        boolean p_435005_
    ) {
        return super.mouseClicked(p_446070_, p_435005_)
            ? true
            : this.realmsNotificationsEnabled()
                && this.realmsNotificationsScreen.mouseClicked(
                    p_446070_,
                    p_435005_
                );
    }

    @Override
    public void removed() {
        if (this.realmsNotificationsScreen != null) {
            this.realmsNotificationsScreen.removed();
        }
    }

    @Override
    public void added() {
        super.added();

        if (this.realmsNotificationsScreen != null) {
            this.realmsNotificationsScreen.added();
        }
    }

    private void confirmDemo(boolean p_96778_) {
        if (p_96778_) {
            try (
                LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess =
                    this.minecraft.getLevelSource().createAccess("Demo_World")
            ) {
                levelstoragesource$levelstorageaccess.deleteLevel();
            } catch (IOException ioexception) {
                SystemToast.onWorldDeleteFailure(this.minecraft, "Demo_World");
                LOGGER.warn("Failed to delete demo world", (Throwable)ioexception);
            }
        }

        this.minecraft.setScreen(this);
    }

    @Override
    public boolean canInterruptWithAnotherScreen() {
        return true;
    }
}
