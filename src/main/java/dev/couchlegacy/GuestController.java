package dev.couchlegacy;

import dev.couchcraft.input.Gamepad;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.control.Controller;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.control.ControllerHandler;
import wily.legacy.client.control.SDLControllerHandler;

/**
 * Shows a guest's Couchcraft pad to Legacy4J as an ordinary Legacy4J controller.
 *
 * Legacy4J and Couchcraft each load their own copy of SDL, so their controller ids can't be
 * matched reliably. Instead of opening the pad a second time, Legacy4J reads the state Couchcraft
 * already polled this frame. Indices are SDL3 gamepad indices, the same ones Legacy4J's SDL
 * handler uses, so every Legacy4J binding (and the player's remapping) works unchanged.
 */
public final class GuestController implements Controller {
	private static final int SDL_AXIS_LEFTX = 0;
	private static final int SDL_AXIS_LEFTY = 1;
	private static final int SDL_AXIS_RIGHTX = 2;
	private static final int SDL_AXIS_RIGHTY = 3;
	private static final int SDL_AXIS_LEFT_TRIGGER = 4;
	private static final int SDL_AXIS_RIGHT_TRIGGER = 5;

	private final Gamepad pad;
	private ControlType type;

	public GuestController(final Gamepad pad) {
		this.pad = pad;
	}

	public Gamepad pad() {
		return this.pad;
	}

	@Override
	public String getName() {
		return this.pad.name();
	}

	@Override
	public ControlType getType() {
		if (this.type == null) {
			this.type = detectType();
		}

		return this.type;
	}

	private ControlType detectType() {
		int sdlType = -1;
		try {
			Field handleField = Gamepad.class.getDeclaredField("handle");
			handleField.setAccessible(true);
			long handle = handleField.getLong(this.pad);
			Method getType = Class.forName("org.lwjgl.sdl.SDLGamepad").getMethod("SDL_GetGamepadType", long.class);
			sdlType = (int)getType.invoke(null, handle);
		} catch (Throwable ignored) {
		}

		// SDL_GamepadType: 2 XBOX360, 3 XBOXONE, 4 PS3, 5 PS4, 6 PS5, 7 SWITCH_PRO, 10 JOYCON_PAIR
		return switch (sdlType) {
			case 2 -> ControlType.getOrEmpty(ControlType.x360);
			case 4 -> ControlType.getOrEmpty(ControlType.PS3);
			case 5 -> ControlType.getOrEmpty(ControlType.PS4);
			case 6 -> ControlType.getOrEmpty(ControlType.PS5);
			case 7, 10 -> ControlType.getOrEmpty(ControlType.SWITCH);
			case 3 -> ControlType.getOrEmpty(ControlType.xONE);
			default -> ControlType.getOrEmpty(ControlType.xONE);
		};
	}

	@Override
	public boolean buttonPressed(final int i) {
		try {
			return i >= 0 && this.pad.isConnected() && this.pad.held(i);
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}

	@Override
	public float axisValue(final int i) {
		if (!this.pad.isConnected()) {
			return 0.0F;
		}

		return switch (i) {
			case SDL_AXIS_LEFTX -> this.pad.leftX();
			case SDL_AXIS_LEFTY -> this.pad.leftY();
			case SDL_AXIS_RIGHTX -> this.pad.rightX();
			case SDL_AXIS_RIGHTY -> this.pad.rightY();
			case SDL_AXIS_LEFT_TRIGGER -> this.pad.leftTrigger();
			case SDL_AXIS_RIGHT_TRIGGER -> this.pad.rightTrigger();
			default -> 0.0F;
		};
	}

	@Override
	public boolean hasButton(final ControllerBinding.Button button) {
		return switch (button) {
			case TOUCHPAD, CAPTURE, LSL, LSR, RSL, RSR -> false;
			default -> true;
		};
	}

	@Override
	public boolean hasAxis(final ControllerBinding.Axis axis) {
		return true;
	}

	@Override
	public void rumble(final char low, final char high, final int durationMs) {
		this.pad.rumble(low / 65535.0F, high / 65535.0F, durationMs);
	}

	@Override
	public ControllerHandler getHandler() {
		// Always SDL3 indices, whatever handler player 1 picked in Legacy4J's settings.
		return SDLControllerHandler.getInstance();
	}
}
