/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zszl.zszlScriptMod.shadowbaritone.api.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class BaritoneToast implements Toast {
    private String title;
    private String subtitle;
    private long firstDrawTime;
    private boolean newDisplay = true;
    private final long totalShowTime;
    private Visibility wantedVisibility = Visibility.SHOW;

    public BaritoneToast(Component titleComponent, Component subtitleComponent, long totalShowTime) {
        this.title = titleComponent.getString();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getString();
        this.totalShowTime = totalShowTime;
    }

    @Override
    public Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager toastGui, long delta) {
        if (this.newDisplay) {
            this.firstDrawTime = delta;
            this.newDisplay = false;
            this.wantedVisibility = Visibility.SHOW;
        }
        if (delta - this.firstDrawTime >= totalShowTime) {
            this.wantedVisibility = Visibility.HIDE;
        }
    }

    @Override
    public void render(GuiGraphics gui, Font font, long delta) {
        gui.blit(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("textures/gui/toasts.png"), 0, 0, 0, 32, 160, 32, 256, 256);

        if (this.subtitle == null) {
            gui.drawString(font, this.title, 18, 12, -11534256);
        } else {
            gui.drawString(font, this.title, 18, 7, -11534256);
            gui.drawString(font, this.subtitle, 18, 18, -16777216);
        }
    }

    public void setDisplayedText(Component titleComponent, Component subtitleComponent) {
        this.title = titleComponent.getString();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getString();
        this.newDisplay = true;
    }

    public static void addOrUpdate(ToastManager toast, Component title, Component subtitle, long totalShowTime) {
        BaritoneToast baritoneToast = toast.getToast(BaritoneToast.class, Toast.NO_TOKEN);

        if (baritoneToast == null) {
            toast.addToast(new BaritoneToast(title, subtitle, totalShowTime));
        } else {
            baritoneToast.setDisplayedText(title, subtitle);
        }
    }

    public static void addOrUpdate(Component title, Component subtitle) {
        addOrUpdate(Minecraft.getInstance().getToastManager(), title, subtitle,
                com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI.getSettings().toastTimer.value);
    }
}
