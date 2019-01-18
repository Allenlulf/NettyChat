package com.allen.chat.protocol;

/**
 * 自定义协议 Instant Messaging Protocol即时协议
 * @author lulf
 * 
 * @date 2019年1月17日
 */
public enum IMP {
	// 系统消息
	SYSTEM("SYSTEM"),
	// 登录命令
	LOGIN("LOGIN"),
	// 登出命令
	LOGOUT("LOGOUT"),
	// 聊天信息
	CHAT("CHAT"),
	// 送鲜花
	FLOWER("FLOWER");

	private String name;

	public static boolean isIMP(String content) {
		return content.matches("^\\[(SYSTEM|LOGIN|LOGOUT|CHAT|FLOWER)\\]");
	}

	IMP(String name) {
		this.name = name();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
