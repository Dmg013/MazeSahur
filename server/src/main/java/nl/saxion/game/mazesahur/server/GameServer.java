package nl.saxion.game.mazesahur.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * Minimal Netty WebSocket server bootstrap. Sets up an HTTP upgrade path at /ws.
 * The actual room/input/state handling is delegated to RoomManager.
 */
public class GameServer {

    private final int port;
    private final RoomManager roomManager;

    public GameServer(final int port) {
        this.port = port;
        this.roomManager = new RoomManager();
    }

    public void start() throws InterruptedException {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            final ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel ch) {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true));
                        pipeline.addLast(new GameWebSocketHandler(roomManager));
                    }
                });

            final ChannelFuture future = bootstrap.bind(port).sync();
            final Channel channel = future.channel();
            System.out.println("[Server] Listening on port " + port + " (path: /ws)");
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
