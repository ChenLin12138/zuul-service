package com.lin.zuul.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

/**
 * @author Chen Lin
 * @date 2019-09-14
 */
@Component
public class ResponseFilter extends ZuulFilter {

	private static final int FILTER_ORDER = 1;
	private static final boolean SHOULD_FILTER = true;
	private static final Logger logger = LoggerFactory.getLogger(ResponseFilter.class);
	
	@Autowired
	FilterUtils filterUtils;
	
	@Override
	public boolean shouldFilter() {
		// TODO Auto-generated method stub
		return SHOULD_FILTER;
	}

	@Override
	public Object run() throws ZuulException {
		RequestContext ctx = RequestContext.getCurrentContext();
		logger.debug("Adding the correlation id to the outbound headers.{}",filterUtils.getCorrelationId());
		//从原始http请求中获取关联id并注入其中
		//之所以敢这样做是因为，对于原始请求中没有关联id的请求在前置过滤器中已经添加了，所以不怕没有
		ctx.getResponse().addHeader(FilterUtils.CORRELATION_ID, filterUtils.getCorrelationId());
		logger.debug("Completing outgoing request for{}",ctx.getRequest().getRequestURI());
		return null;
	}

	@Override
	public String filterType() {
		// TODO Auto-generated method stub
		return FilterUtils.POST_FILTER_TYPE;
	}

	@Override
	public int filterOrder() {
		// TODO Auto-generated method stub
		return FILTER_ORDER;
	}

}
