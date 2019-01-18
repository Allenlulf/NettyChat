package com.allen.chat.server.handler;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * http 处理器
 * 
 * @author lulf
 * @date 2019年1月17日
 */
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private URL baseURL = HttpHandler.class.getProtectionDomain().getCodeSource().getLocation();

	// classpath
	private final String WEB_ROOT = "webroot";

	private File getFileFromRoot(String fileName) throws URISyntaxException {
		String path = baseURL.toURI() + WEB_ROOT + "/" + fileName;
		path = !path.contains("file:") ? path : path.substring(5);
		path = path.replaceAll("//", "/");
		return new File(path);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		// 在netty中只要是方法后面加个0的，都是实现类的方法，不是接口方法
		// 在netty中提供了非常丰富的工具类
		// 1)获取客户端请求的url
		String uri = request.getUri();
		// 默认让他跳转chat
		String page = uri.equals("/") ? "chat.html" : uri;
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(getFileFromRoot(page), "r");
		} catch (Exception e) {
			ctx.fireChannelRead(request.retain());
			return;
		}
		String contextType = "";
		if (page.endsWith(".html")) {
			contextType = "text/html;";
		}
		if (uri.endsWith(".css")) {
			contextType = "text/css;";
		} else if (uri.endsWith(".js")) {
			contextType = "text/javascript;";
		} else if (uri.toLowerCase().matches("(jpg|png|gif|ico)$")) {
			String ext = uri.substring(uri.lastIndexOf("."));
			contextType = "image/;" + ext + ";";
		}
		HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, contextType + "charset=utf-8;");
		// 判断是不是长连接
		boolean keepalive = HttpHeaders.isKeepAlive(request);
		if (keepalive) {
			response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length());
			response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		ctx.write(response);

		ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));

		ChannelFuture f = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		// 如果不是长连接的话要清空缓冲区
		if (!keepalive) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
		file.close();
	}
}
