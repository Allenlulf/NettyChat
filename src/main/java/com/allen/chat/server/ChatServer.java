package com.allen.chat.server;

import com.allen.chat.server.handler.HttpHandler;
import com.allen.chat.server.handler.WebSocketHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 聊天室chat 服务器
 * 
 * @author lulf
 * @date 2019年1月17日
 */
public class ChatServer {

	private int port = 8080;

	/**
	 * 
	 * @param port
	 * @throws Exception
	 */
	public void start() throws Exception {
		// boss线程
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		// worker线程
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			//启动引擎
			ServerBootstrap b = new ServerBootstrap();
			//主从模型
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						public void initChannel(SocketChannel ch) throws Exception {
							//所有自定义业务从这开始
							ChannelPipeline pipeline = ch.pipeline();
							//==============支持 http协议===============
							//解码和编码http请求
							pipeline.addLast(new HttpServerCodec());
							pipeline.addLast(new HttpObjectAggregator(64 * 1024));
							//用于处理文件流的handler
							pipeline.addLast(new ChunkedWriteHandler());
							pipeline.addLast(new HttpHandler());
							//=============支持websocket协议============
							pipeline.addLast(new WebSocketServerProtocolHandler("/im"));
							pipeline.addLast(new WebSocketHandler());
						}
					}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
			// 绑定服务端口,等待客户端连接
			ChannelFuture f = b.bind(port).sync();
			System.out.println("服务已启动，监听端口:" + port);
			// 开始接收客户
			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) {
		try {
			new ChatServer().start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
