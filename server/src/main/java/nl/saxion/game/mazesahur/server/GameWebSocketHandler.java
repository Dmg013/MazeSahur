package nl.saxion.game.mazesahur.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * Handles incoming WebSocket text frames and delegates to the RoomManager.
 */
public class GameWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final RoomManager roomManager;

    public GameWebSocketHandler(final RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        roomManager.handleConnect(ctx.channel());
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) {
        roomManager.handleDisconnect(ctx.channel());
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final TextWebSocketFrame msg) {
        roomManager.handleMessage(ctx.channel(), msg.text());
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        roomManager.handleError(ctx.channel(), cause);
        ctx.close();
    }
}
