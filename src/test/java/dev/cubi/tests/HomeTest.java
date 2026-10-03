package dev.cubi.tests;

import dev.cubi.ui.DeckLayout.Home;
import dev.cubi.ui.DeckLayout.Rect;
import dev.cubi.ui.HomeState;
import java.util.Arrays;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Main-menu hit regions and input dispatch, without constructing Minecraft or OpenGL. */
public final class HomeTest {
    private static int assertions;
    private HomeTest() { }
    public static int run() throws Exception {
        assertions = 0;
        geometry(); navigation(); dispatch();
        return assertions;
    }
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static void geometry() {
        for (int[] size : new int[][] {{320, 240}, {426, 240}, {600, 360}, {854, 480}, {1920, 1080}, {3440, 1440}, {240, 320}}) {
            Home home = new Home(size[0], size[1]);
            boolean fits = true, separate = true, clickable = true;
            for (int i = 0; i < home.controls.length; i++) {
                Rect r = home.controls[i];
                fits &= r.x >= 0 && r.y >= 0 && (r.x + r.w) * home.scale <= size[0]
                        && (r.y + r.h) * home.scale <= size[1];
                clickable &= home.hit(Math.round(r.centerX() * home.scale), Math.round(r.centerY() * home.scale)) == i;
                for (int j = 0; j < i; j++) separate &= !r.intersects(home.controls[j]);
            }
            check(fits && separate && clickable, "All home controls fit, remain disjoint and clickable at " + size[0] + "x" + size[1]);
            check(home.hit(-1, -1) == -1 && home.hit(size[0], size[1]) == -1, "Outside clicks cannot activate a home action");
            Rect play = home.controls[Home.PLAY], servers = home.controls[Home.SERVERS], customize = home.controls[Home.CUSTOMIZE];
            check(play.x == servers.x && play.x == customize.x && play.w == 220 && servers.w == play.w && customize.w == play.w
                    && play.y + play.h < servers.y && servers.y + servers.h < customize.y,
                    "Game and customization actions form one centered column");
            check(Math.abs(play.centerX() - home.width / 2) <= 1 && home.title.x >= 0
                    && home.title.x + home.title.w <= home.width && home.title.y + home.title.h < home.brandY
                    && home.brandY + 16 < play.y && home.controls[Home.LANGUAGE].y + 18 < home.height - 14,
                    "Minecraft title, Cubi brand, actions and footer have separate space");
            check(play.h * home.scale >= 15 && (size[0] < 320 || size[1] < 260 || home.scale == 1),
                    "Normal GUI sizes keep vanilla-sized controls; narrow screens retain readable targets");
        }
    }
    private static void navigation() {
        HomeState state = new HomeState();
        Object parent = new Object(), child = new Object();
        boolean[] enabled = new boolean[Home.COUNT];
        Arrays.fill(enabled, true);
        enabled[Home.REALMS] = false;
        state.bind(parent);
        check(state.focus() == -1 && state.take(parent) == -1, "Home waits for explicit input");
        state.move(-1, enabled);
        check(state.focus() == Home.LANGUAGE, "Reverse Tab skips disabled Realms and starts at the last available action");
        state.move(1, enabled);
        check(state.focus() == Home.PLAY, "Tab wraps to Singleplayer");
        state.clearFocus(); state.move(1, enabled);
        check(state.focus() == Home.PLAY, "First Tab selects Singleplayer");
        state.move(1, enabled);
        check(state.focus() == Home.SERVERS, "Next Tab selects Multiplayer");
        state.move(1, enabled);
        check(state.focus() == Home.CUSTOMIZE, "Customization follows the two game actions");
        state.request(Home.REALMS, enabled); state.request(-1, enabled); state.request(Home.COUNT, enabled);
        check(state.take(parent) == -1, "Disabled and invalid actions do not get queued");
        state.request(Home.CUSTOMIZE, enabled); state.request(Home.QUIT, enabled);
        check(state.take(parent) == Home.CUSTOMIZE && state.take(parent) == -1, "Several events in one tick cannot trigger two actions");
        state.request(Home.OPTIONS, enabled);
        check(state.take(child) == -1 && state.take(parent) == -1, "Changing screens cancels the queued action permanently");
        state.request(Home.PLAY, enabled); state.bind(child);
        check(state.focus() == -1 && state.take(child) == -1, "A new main-menu instance resets focus and pending input");
        Arrays.fill(enabled, false); state.move(1, enabled);
        check(state.focus() == -1, "An unavailable control set has no keyboard activation target");
    }
    private static void dispatch() throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader("dev.cubi.ui.HomeScreen").accept(node, 0);
        boolean clickQueues = false, keyQueues = false, repeatsGuarded = false, took = false, opensAfterTake = false;
        boolean opensInInput = false;
        for (Object item : node.methods) {
            MethodNode method = (MethodNode) item;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                boolean input = method.name.equals("click") || method.name.equals("type");
                boolean queues = call.owner.equals("dev/cubi/ui/HomeState") && call.name.equals("request");
                boolean opens = (call.owner.equals("dev/cubi/core/CubiClient") && call.name.equals("open"))
                        || (call.owner.equals("dev/cubi/bridge/Game189") && (call.name.equals("show") || call.name.equals("menuAction") || call.name.equals("resources")));
                clickQueues |= method.name.equals("click") && queues;
                keyQueues |= method.name.equals("type") && queues;
                opensInInput |= input && opens;
                repeatsGuarded |= method.name.equals("type") && call.owner.equals("org/lwjgl/input/Keyboard") && call.name.equals("isRepeatEvent");
                if (method.name.equals("tick")) {
                    took |= call.owner.equals("dev/cubi/ui/HomeState") && call.name.equals("take");
                    opensAfterTake |= took && opens;
                }
            }
        }
        check(clickQueues && keyQueues && !opensInInput && repeatsGuarded && opensAfterTake,
                "Home mouse/keyboard queue screen changes; only tick opens them after repeat guards");
    }
}
