package com.allen.chat.server.handler;

import com.allen.chat.process.IMProcessor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * websocket 处理登录登出
 * 
 * @author lulf
 * @date 2019年1月17日
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	private IMProcessor processor = new IMProcessor();

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
		processor.process(ctx.channel(), msg.text());

	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		processor.logout(ctx.channel());
	}
}
