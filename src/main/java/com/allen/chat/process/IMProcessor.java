package com.allen.chat.process;

import com.alibaba.fastjson.JSONObject;
import com.allen.chat.protocol.IMDecoder;
import com.allen.chat.protocol.IMEncoder;
import com.allen.chat.protocol.IMMessage;
import com.allen.chat.protocol.IMP;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 处理聊天
 * 
 * @author lulf
 * @date 2019年1月17日
 */
public class IMProcessor {
	//记录在线人员
	private final static ChannelGroup onlineusers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	// 自定义编码器
	private IMDecoder decoder = new IMDecoder();
	// 自定义解码器
	private IMEncoder encoder = new IMEncoder();

	private final AttributeKey<String> NICK_NAME = AttributeKey.valueOf("nickName");
	private final AttributeKey<String> IP_ADDR = AttributeKey.valueOf("ipAddr");
	private final AttributeKey<JSONObject> ATTRS = AttributeKey.valueOf("attrs");

	/**
	 * 登出
	 * 
	 * @param client
	 */
	public void logout(Channel client) {
		onlineusers.remove(client);
	}

	public void process(Channel client, String msg) {
		System.out.println(msg);
		IMMessage request = decoder.decode(msg);
		if (null == request) {
			return;
		}
		String nickName = request.getSender();
		// 这边对request做下判断，如果是登录动作的话，就往onlineUsers中加入
		if (IMP.LOGIN.getName().equals(request.getCmd())) {
			client.attr(NICK_NAME).getAndSet(request.getSender());
			onlineusers.add(client);
			// 循环所有的客户端，通知所有人
			for (Channel channel : onlineusers) {
				if (channel != client) {
					request = new IMMessage(IMP.SYSTEM.getName(), sysTime(), onlineusers.size(),
							nickName + "加入聊天室");
				} else {
					request = new IMMessage(IMP.SYSTEM.getName(), sysTime(), onlineusers.size(), "已与服务器建立连接");
				}
				String Text = encoder.encode(request);
				channel.writeAndFlush(new TextWebSocketFrame(Text));
			}
		}
		// 退出动作,就往onlineUsers中去除此对象
		else if (IMP.LOGOUT.getName().equals(request.getCmd())) {
			onlineusers.remove(client);
		}
		// 聊天
		else if (IMP.CHAT.getName().equals(request.getCmd())) {
			// 循环所有的客户端，通知所有人
			for (Channel channel : onlineusers) {
				if (channel != client) {
					request.setSender(client.attr(NICK_NAME).get());
				} else {
					request.setSender("you");
				}

				String Text = encoder.encode(request);
				channel.writeAndFlush(new TextWebSocketFrame(Text));
			}
		}
		//鲜花特效
		else if (IMP.FLOWER.getName().equals(request.getCmd())) {
			//客户端发送 刷鲜花的命令
			JSONObject attrs = getAttrs(client);
			long currTime = sysTime();
			if (null != attrs) {
				long lastTime = attrs.getLongValue("lastFlowerTime");
				//10秒之内不允许重复刷鲜花,防止有人恶意刷鲜花  by Allen 2019年1月17日
				int secends = 10;
				long sub = currTime - lastTime;
				if (sub < 1000 * secends) {
					request.setSender("you");
					request.setCmd(IMP.SYSTEM.getName());
					request.setContent("您送鲜花太频繁," + (secends - Math.round(sub / 1000)) + "秒后再试");
					String content = encoder.encode(request);
					client.writeAndFlush(new TextWebSocketFrame(content));
					return;
				}
			}

			//正常送花
			for (Channel channel : onlineusers) {
				if (channel == client) {
					request.setSender("you");
					request.setContent("你给大家送了一波鲜花雨");
					//设置最后一次刷鲜花的时间，防止你刷花刷的太频繁
					setAttrs(client, "lastFlowerTime", currTime);
				} else {
					request.setSender(client.attr(NICK_NAME).get());
					request.setContent(client.attr(NICK_NAME).get() + "送来一波鲜花雨");
				}
				request.setTime(sysTime());

				String content = encoder.encode(request);
				channel.writeAndFlush(new TextWebSocketFrame(content));
			}
		}
	}

	/**
	 * 获取扩展属性
	 * 
	 * @param client
	 * @return
	 */
	public JSONObject getAttrs(Channel client) {
		try {
			return client.attr(ATTRS).get();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 获取扩展属性
	 * 
	 * @param client
	 * @return
	 */
	private void setAttrs(Channel client, String key, Object value) {
		try {
			JSONObject json = client.attr(ATTRS).get();
			json.put(key, value);
			client.attr(ATTRS).set(json);
		} catch (Exception e) {
			JSONObject json = new JSONObject();
			json.put(key, value);
			client.attr(ATTRS).set(json);
		}
	}

	private Long sysTime() {
		return System.currentTimeMillis();
	}
}
