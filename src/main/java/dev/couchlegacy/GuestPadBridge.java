package dev.couchlegacy;

import dev.couchcraft.context.PlayerContext;
import dev.couchcraft.input.Gamepad;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Util;
import wily.legacy.client.control.BindingState;
import wily.legacy.client.control.Controller;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.control.ControllerManager;

/**
 * Lets Legacy4J run every guest's controller, the same way it runs player 1's.
 *
 * Legacy4J keeps one controller state for the whole game: one ControllerManager and one state per
 * button. Each player gets a private copy of that state, swapped in and out together with the rest
 * of Couchcraft's per-player state. Every frame, inside the guest's own scope, Legacy4J's normal
 * update runs on the guest's pad, so guests get Legacy4J's menu navigation, button layout,
 * remapping, camera and prompts instead of Couchcraft's mouse-cursor controls.
 */
public final class GuestPadBridge {
	/** Per-player ControllerManager fields. Everything else in it (poller, timing, names) stays shared. */
	private static final String[] MANAGER_FIELDS = {
		"connectedController", "activeControllerSlot", "isCursorDisabled", "resetCursor", "simulateShift",
		"canChangeSlidersValue", "isControllerSimulatingInput", "blockNextCharType", "isControllerTheLastInput",
		"gyroController"
	};

	private static final Map<PlayerContext, Snapshot> SNAPSHOTS = new WeakHashMap<>();
	private static final Map<PlayerContext, GuestController> CONTROLLERS = new WeakHashMap<>();
	private static final Map<Class<?>, Field[]> STATE_FIELDS = new HashMap<>();
	private static Field[] managerFields;
	private static boolean broken;
	private static long lastError;

	private GuestPadBridge() {
	}

	private record Snapshot(Object[] manager, Map<BindingState, Object[]> states) {
	}

	// ---------------------------------------------------------------- state swap

	public static void save(final PlayerContext context) {
		if (broken) {
			return;
		}

		try {
			SNAPSHOTS.put(context, capture());
		} catch (Throwable t) {
			fail("save", t);
		}
	}

	public static void load(final PlayerContext context) {
		if (broken) {
			return;
		}

		try {
			Snapshot snapshot = SNAPSHOTS.get(context);
			if (snapshot == null) {
				if (context.primary) {
					return;
				}

				snapshot = freshGuest();
				SNAPSHOTS.put(context, snapshot);
			}

			apply(snapshot);
		} catch (Throwable t) {
			fail("load", t);
		}
	}

	private static Snapshot capture() throws IllegalAccessException {
		ControllerManager manager = ControllerManager.getInstance();
		Field[] fields = managerFields();
		Object[] values = new Object[fields.length];
		for (int i = 0; i < fields.length; i++) {
			values[i] = fields[i].get(manager);
		}

		Map<BindingState, Object[]> states = new IdentityHashMap<>();
		for (ControllerBinding<?> binding : ControllerBinding.map.values()) {
			BindingState state = binding.state();
			Field[] stateFields = stateFields(state.getClass());
			Object[] stateValues = new Object[stateFields.length];
			for (int i = 0; i < stateFields.length; i++) {
				stateValues[i] = stateFields[i].get(state);
			}

			states.put(state, stateValues);
		}

		return new Snapshot(values, states);
	}

	private static void apply(final Snapshot snapshot) throws IllegalAccessException {
		ControllerManager manager = ControllerManager.getInstance();
		Field[] fields = managerFields();
		for (int i = 0; i < fields.length; i++) {
			fields[i].set(manager, snapshot.manager[i]);
		}

		for (Map.Entry<BindingState, Object[]> entry : snapshot.states.entrySet()) {
			Field[] stateFields = stateFields(entry.getKey().getClass());
			Object[] values = entry.getValue();
			for (int i = 0; i < stateFields.length; i++) {
				stateFields[i].set(entry.getKey(), values[i]);
			}
		}
	}

	/** A guest starts with player 1's settings-like flags but nothing pressed and no controller yet. */
	private static Snapshot freshGuest() throws IllegalAccessException {
		Snapshot base = capture();
		Field[] fields = managerFields();
		for (int i = 0; i < fields.length; i++) {
			switch (fields[i].getName()) {
				case "connectedController", "gyroController" -> base.manager[i] = null;
				case "activeControllerSlot" -> base.manager[i] = -1;
				case "isControllerTheLastInput" -> base.manager[i] = true;
				case "isCursorDisabled" -> base.manager[i] = true;
				case "resetCursor", "simulateShift", "isControllerSimulatingInput", "blockNextCharType" -> base.manager[i] = false;
				default -> {
				}
			}
		}

		for (Map.Entry<BindingState, Object[]> entry : base.states.entrySet()) {
			Field[] stateFields = stateFields(entry.getKey().getClass());
			Object[] values = entry.getValue();
			for (int i = 0; i < stateFields.length; i++) {
				Class<?> type = stateFields[i].getType();
				String name = stateFields[i].getName();
				if (type == boolean.class) {
					values[i] = false;
				} else if (type == int.class) {
					values[i] = name.equals("timePressed") || name.equals("previousTimePressed") ? -1 : 0;
				} else if (type == float.class) {
					values[i] = 0.0F;
				} else if (type == double.class) {
					values[i] = 0.0;
				} else if (type == long.class) {
					values[i] = 0L;
				}
			}
		}

		return base;
	}

	private static Field[] managerFields() {
		if (managerFields == null) {
			List<Field> found = new ArrayList<>();
			for (String name : MANAGER_FIELDS) {
				try {
					Field field = ControllerManager.class.getDeclaredField(name);
					field.setAccessible(true);
					found.add(field);
				} catch (NoSuchFieldException e) {
					CouchLegacyBridge.LOGGER.warn("[couchlegacy] Legacy4J ControllerManager has no field {}; skipping it", name);
				}
			}

			managerFields = found.toArray(new Field[0]);
		}

		return managerFields;
	}

	private static Field[] stateFields(final Class<?> type) {
		return STATE_FIELDS.computeIfAbsent(type, t -> {
			List<Field> found = new ArrayList<>();
			for (Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()) {
				for (Field field : c.getDeclaredFields()) {
					int mod = field.getModifiers();
					if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || field.isSynthetic()) {
						continue;
					}

					field.setAccessible(true);
					found.add(field);
				}
			}

			return found.toArray(new Field[0]);
		});
	}

	// ---------------------------------------------------------------- driving a guest's pad

	/** Runs inside the guest's scope, in place of Couchcraft's own pad handling. */
	public static void drive(final PlayerContext context) {
		if (broken) {
			return;
		}

		Gamepad pad = context.pad;
		ControllerManager manager = ControllerManager.getInstance();
		try {
			if (pad == null || !pad.isConnected()) {
				if (manager.connectedController instanceof GuestController) {
					manager.updateBindings(Controller.EMPTY);
					manager.connectedController = null;
				}

				CONTROLLERS.remove(context);
				return;
			}

			GuestController controller = CONTROLLERS.get(context);
			if (controller == null || controller.pad() != pad) {
				controller = new GuestController(pad);
				CONTROLLERS.put(context, controller);
			}

			manager.connectedController = controller;
			manager.updateBindings(controller);
		} catch (Throwable t) {
			long now = Util.getMillis();
			if (now - lastError > 5000L) {
				lastError = now;
				CouchLegacyBridge.LOGGER.warn("[couchlegacy] Legacy4J update for a guest controller failed", t);
			}
		}
	}

	public static boolean isBroken() {
		return broken;
	}

	private static void fail(final String what, final Throwable t) {
		broken = true;
		CouchLegacyBridge.LOGGER.error("[couchlegacy] Could not {} Legacy4J controller state; guest controller bridge disabled", what, t);
	}
}
