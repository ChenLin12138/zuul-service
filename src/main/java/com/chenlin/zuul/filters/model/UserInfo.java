package com.chenlin.zuul.filters.model;

/**
 * @author Chen Lin
 * @date 2019-09-18
 */

public class UserInfo {

	String organizationId;
	String userId;

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

}
