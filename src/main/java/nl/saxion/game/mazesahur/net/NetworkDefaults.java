package nl.saxion.game.mazesahur.net;

/**
 * Default network settings for quick testing; override with environment variables.
 */
public final class NetworkDefaults {
    private NetworkDefaults() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String serverUrl() {
/Users/sympact/Library/Java/JavaVirtualMachines/ms-21.0.9/Contents/Home/bin/java -XstartOnFirstThread -javaagent:/Users/sympact/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=55276 -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath /Users/sympact/Desktop/MazeSahur/build/classes/java/main:/Users/sympact/Desktop/MazeSahur/build/resources/main:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.gitlab.evertduipmans/saxiongameapp/1.0.1/27d2aa1e3e20d051998a237aeb2db4e983c8c624/saxiongameapp-1.0.1.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/com.github.mgsx-dev.gdx-gltf/core/2.2.1/2499ab036fa52d98f320aba6f70cc1f556f0c220/core-2.2.1.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-databind/2.17.2/e6deb029e5901e027c129341fac39e515066b68c/jackson-databind-2.17.2.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/com.github.mgsx-dev.gdx-gltf/gltf/2.2.1/8d43331492ffdb51e56654d13a6b162667427173/gltf-2.2.1.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx/gdx/1.9.11/69fe46897eed6f92e3f01b75cc7ca5f860881a8b/gdx-1.9.11.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-annotations/2.17.2/147b7b9412ffff24339f8aba080b292448e08698/jackson-annotations-2.17.2.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-core/2.17.2/969a35cb35c86512acbadcdbbbfb044c877db814/jackson-core-2.17.2.jar:/Users/sympact/Desktop/MazeSahur/server/build/classes/java/main:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-all/4.1.109.Final/3ba1acc8ff088334f2ac5556663f8b737eb8b571/netty-all-4.1.109.Final.jar:/Users/sympact/Desktop/MazeSahur/build/libs/MazeSahur-1.0.0.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-buffer/4.1.109.Final/9d21d602ad7c639fa16b1d26559065d310a34c51/netty-buffer-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec/4.1.109.Final/16e0b2beb49318a549d3ba5d66d707bd5daa8c97/netty-codec-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-dns/4.1.109.Final/ee231baee2cc9f1300ecc0d9a1e8bb9b31db02fa/netty-codec-dns-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-haproxy/4.1.109.Final/d4caff157b23609281a9cfdf24496973bee0c7d4/netty-codec-haproxy-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-http/4.1.109.Final/6dca43cedc0b2dc6bf57bdc85fce6ffca3e6b72a/netty-codec-http-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-http2/4.1.109.Final/6bd4a54b69a81356393f6e4621bad40754f8a5a2/netty-codec-http2-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-memcache/4.1.109.Final/287eb5585f23c325c4dec5bbd227c479d7c6c76a/netty-codec-memcache-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-mqtt/4.1.109.Final/66bc6a99872a982bb810ac5990fed462c67c2082/netty-codec-mqtt-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-redis/4.1.109.Final/d7b7ec8347585db59dabc33ec1fe1d136312ac82/netty-codec-redis-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-smtp/4.1.109.Final/183ef9965024fc63028f7bc6abbec8dd75689dba/netty-codec-smtp-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-socks/4.1.109.Final/7f4f0c0dd54c578af2c613a0db7172bf7dca9c79/netty-codec-socks-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-stomp/4.1.109.Final/3deb2116225e060f340a414ec78d71ba056d59f1/netty-codec-stomp-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-codec-xml/4.1.109.Final/1e92610f367475a12cf6663415fa6e694f37d430/netty-codec-xml-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-common/4.1.109.Final/da63e54ee1ca69abf4206cb74fadef7f50850911/netty-common-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-handler/4.1.109.Final/9167863307b3c44cc12262e7b5512de3499b9c4a/netty-handler-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-native-unix-common/4.1.109.Final/da7fe1e6943cbab8ee48df2beadc2c8304f347a2/netty-transport-native-unix-common-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-handler-proxy/4.1.109.Final/a77224107f586a7f9e3dc5d12fc0d4d8f0c04803/netty-handler-proxy-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-handler-ssl-ocsp/4.1.109.Final/c630358d6aa967795cf2d07f95e454dbd54056e4/netty-handler-ssl-ocsp-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-resolver/4.1.109.Final/55485ac976e27c8bb67ee111a8490c58f67b70c/netty-resolver-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-resolver-dns/4.1.109.Final/5f4d858234b557b73631a24e562bb89fc5399cad/netty-resolver-dns-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport/4.1.109.Final/79e3b07d58ef03c7a860d48f932b720675aa8bd3/netty-transport-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-rxtx/4.1.109.Final/5993d957f3ec12a4e9b3b03ccfe92835bf361b1d/netty-transport-rxtx-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-sctp/4.1.109.Final/50d000465ccc77a680b1bbaa74bde291197f2132/netty-transport-sctp-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-udt/4.1.109.Final/4c9818b7e1bc77cdae3a16ec2cb263e98941aa46/netty-transport-udt-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-classes-epoll/4.1.109.Final/7307c8acbc9b331fce3496750a5112bdc726fd2a/netty-transport-classes-epoll-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-classes-kqueue/4.1.109.Final/ea8fb7dca544f9ddda3e6cf5ec5bbb0ca81c5e88/netty-transport-classes-kqueue-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-resolver-dns-classes-macos/4.1.109.Final/6badcddb9885324b1a2b396068cb45c9af02163b/netty-resolver-dns-classes-macos-4.1.109.Final.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-native-epoll/4.1.109.Final/2f95102cdc2b0d86d6191bd8642706375fec2662/netty-transport-native-epoll-4.1.109.Final-linux-aarch_64.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-native-epoll/4.1.109.Final/64e43e84d907d057da2b03bd9499a2c72f70c6df/netty-transport-native-epoll-4.1.109.Final-linux-riscv64.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-native-epoll/4.1.109.Final/1f762ef557cf91d16b86bde840f618ddb38e6f81/netty-transport-native-epoll-4.1.109.Final-linux-x86_64.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-native-kqueue/4.1.109.Final/211c6e474afb60baf9c4b2b0d736444edfac0f3a/netty-transport-native-kqueue-4.1.109.Final-osx-aarch_64.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-transport-native-kqueue/4.1.109.Final/22ce4247f1a90fbb939f4aa68d66744b1efbe455/netty-transport-native-kqueue-4.1.109.Final-osx-x86_64.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-resolver-dns-native-macos/4.1.109.Final/e2f994d2ea578c0f24b1100b67dd94701355e140/netty-resolver-dns-native-macos-4.1.109.Final-osx-aarch_64.jar:/Users/sympact/.gradle/caches/modules-2/files-2.1/io.netty/netty-resolver-dns-native-macos/4.1.109.Final/fd5b9a5723287b292123f2fe6811b68a6a9783b5/netty-resolver-dns-native-macos-4.1.109.Final-osx-x86_64.jar nl.saxion.game.Main
[SplashScreen] Could not set always on top: com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.getWindow()
[SplashScreen] Window set to borderless and centered
[SplashScreen] Displayed - game will load on render thread
[SplashScreen] Stage 0: Initializing...
[SplashScreen] Stage 1: Loading materials...
[ResourceManager] Loading materials...
[TextureSet] Loaded: textures/wall/red_brick_plaster_patch_02_diff_4k.png
[TextureSet] Loaded: textures/wall/red_brick_plaster_patch_02_nor_gl_4k.png
[TextureSet] Loaded: textures/wall/red_brick_plaster_patch_02_ao_4k.png
[TextureSet] Loaded: textures/wall/red_brick_plaster_patch_02_rough_4k.png
[TextureSet] Loaded: textures/wall/red_brick_plaster_patch_02_disp_4k.png
[TextureSet] Loaded: textures/wall/red_brick_plaster_patch_02_spec_4k.png
[MaterialManager] Loaded WALL textures
[TextureSet] Loaded: textures/floor/concrete_floor_damaged_01_diff_4k.jpg
[TextureSet] Loaded: textures/floor/concrete_floor_damaged_01_nor_gl_4k.jpg
[TextureSet] Loaded: textures/floor/concrete_floor_damaged_01_ao_4k.jpg
[TextureSet] Loaded: textures/floor/concrete_floor_damaged_01_rough_4k.jpg
[TextureSet] Loaded: textures/floor/concrete_floor_damaged_01_disp_4k.png
[MaterialManager] Loaded FLOOR textures
[TextureSet] Loaded: textures/roof/concrete_layers_diff_4k.jpg
[TextureSet] Loaded: textures/roof/concrete_layers_nor_gl_4k.png
[TextureSet] Loaded: textures/roof/concrete_layers_rough_4k.png
[TextureSet] Loaded: textures/roof/concrete_layers_disp_4k.png
[MaterialManager] Loaded ROOF textures
[ResourceManager] Materials loaded in 4150ms
[SplashScreen] Stage 2: Loading character models...
[ResourceManager] Loading character models...
[ResourceManager]   Loaded character: Default
[ResourceManager]   Loaded character: Big Business
[ResourceManager]   Loaded character: Soundcloud
[ResourceManager]   Loaded character: Lockdown
[ResourceManager]   Loaded character: Maximilian
[ResourceManager] Character models loaded in 10988ms
[SplashScreen] Stage 3: Loading audio...
[ResourceManager] Loading audio...
[ResourceManager]   Loaded audio: flashlight_toggle
[ResourceManager] Audio loaded in 55ms
[SplashScreen] All assets loaded!
[SplashScreen] Loading complete, transitioning to menu...
[SplashScreen] Executing transition...
[MenuScreen] Enhanced main menu initialized
[MenuScreen] Multiplayer button clicked - Showing form
[CharacterSelectionScreen] Showing modern character selection
[CharacterSelectionScreen] Using pre-loaded model for Maximilian
[CharacterSelectionScreen] Using pre-loaded model for Big Business
[CharacterSelectionScreen] Using pre-loaded model for Soundcloud
[CharacterSelectionScreen] Using pre-loaded model for Lockdown
[CharacterSelectionScreen] Using pre-loaded model for Default
[CharacterSelectionScreen] Selected character: MAXIMILIAN
Rail network built: 314 nodes
  Junctions: 68
  Corridors: 234
  Dead ends: 12
[GameScreen] ===== PHOTO FRAME SPAWN DEBUG =====
[GameScreen] Created 58 photo frames throughout maze
[GameScreen] =====================================
[GameScreen] ===== BOOST PICKUP SPAWN DEBUG =====
[GameScreen] Boost 1 spawned at (124.0, 60.0)
[GameScreen] Boost 2 spawned at (164.0, 60.0)
[GameScreen] Boost 3 spawned at (28.0, 60.0)
[GameScreen] Boost 4 spawned at (68.0, 140.0)
[GameScreen] Boost 5 spawned at (84.0, 108.0)
[GameScreen] Boost 6 spawned at (172.0, 28.0)
[GameScreen] Boost 7 spawned at (52.0, 188.0)
[GameScreen] Boost 8 spawned at (36.0, 140.0)
[GameScreen] Created 8 boost pickups
[GameScreen] =========================================
[GameScreen] Using maze seed: -1226348203 networked=true character=MAXIMILIAN
[GameScreen] Initializing for the first time...
[GameScreen] Screen dimensions at show(): 2560x1440
[MultiplayerSession] Joined room test-room as 2204237f-2101-4fd8-ac2a-92d6c2700dcd level=1 seed=-1226348203 exit=(12.0,12.0)
[MultiplayerSession] Level changed to 2 seed=1767612799860
[GameScreen] ====== LEVEL CHANGE TO LEVEL 2 ======
[SpotlightShader] Precompiling shader (may take a moment on low-end systems)...
[SpotlightShader] Shader fully precompiled and ready for optimal performance
[GameScreen] Using pre-loaded materials from ResourceManager
[MazeRenderer] Loading 58 photo frames...
FATAL ERROR in native method: Thread[#28,HttpClient-1-Worker-0,5,main]: No context is current or a function that is not available in the current context was called. The JVM will abort execution.
	at org.lwjgl.opengl.GL11C.nglGetIntegerv(Native Method)
	at org.lwjgl.opengl.GL11C.glGetIntegerv(GL11C.java:836)
	at org.lwjgl.opengl.GL11.glGetIntegerv(GL11.java:2604)
	at com.badlogic.gdx.backends.lwjgl3.Lwjgl3GL20.glGetIntegerv(Lwjgl3GL20.java:406)
	at com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder.getMaxTextureUnits(DefaultTextureBinder.java:74)
	at com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder.<init>(DefaultTextureBinder.java:62)
	at com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder.<init>(DefaultTextureBinder.java:58)
	at com.badlogic.gdx.graphics.g3d.ModelBatch.<init>(ModelBatch.java:82)
	at com.badlogic.gdx.graphics.g3d.ModelBatch.<init>(ModelBatch.java:125)
	at nl.saxion.game.mazesahur.rendering.MazeRenderer.initialize(MazeRenderer.java:154)
	at nl.saxion.game.mazesahur.screen.GameScreen.handleLevelChange(GameScreen.java:1037)
	at nl.saxion.game.mazesahur.screen.GameScreen$1.onLevelChanged(GameScreen.java:175)
	at nl.saxion.game.mazesahur.net.MultiplayerSession.handleLevelChange(MultiplayerSession.java:267)
	at nl.saxion.game.mazesahur.net.MultiplayerSession.onMessage(MultiplayerSession.java:180)
	at nl.saxion.game.mazesahur.net.NetworkClient.onText(NetworkClient.java:96)
	at jdk.internal.net.http.websocket.WebSocketImpl$ReceiveTask.processText(java.net.http@21.0.9/WebSocketImpl.java:635)
	at jdk.internal.net.http.websocket.WebSocketImpl$ReceiveTask.run(java.net.http@21.0.9/WebSocketImpl.java:443)
	at jdk.internal.net.http.common.SequentialScheduler$CompleteRestartableTask.run(java.net.http@21.0.9/SequentialScheduler.java:149)
	at jdk.internal.net.http.common.SequentialScheduler$TryEndDeferredCompleter.complete(java.net.http@21.0.9/SequentialScheduler.java:324)
	at jdk.internal.net.http.common.SequentialScheduler$CompleteRestartableTask.run(java.net.http@21.0.9/SequentialScheduler.java:151)
	at jdk.internal.net.http.common.SequentialScheduler$SchedulableTask.run(java.net.http@21.0.9/SequentialScheduler.java:207)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(java.base@21.0.9/ThreadPoolExecutor.java:1144)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(java.base@21.0.9/ThreadPoolExecutor.java:642)
	at java.lang.Thread.runWith(java.base@21.0.9/Thread.java:1596)
	at java.lang.Thread.run(java.base@21.0.9/Thread.java:1583)

Process finished with exit code 134 (interrupted by signal 6:SIGABRT)

            return System.getenv().getOrDefault("MAZE_SERVER_URL", "ws://45.94.80.147:27024/ws");
    }

    public static String room() {
        return System.getenv().getOrDefault("MAZE_ROOM", "test-room");
    }

    public static String playerName() {
        return System.getenv().getOrDefault("MAZE_PLAYER_NAME", "Player" + (int) (Math.random() * 1000));
    }
}
