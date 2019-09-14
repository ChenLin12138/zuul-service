package com.chenlin.zuul.filters;

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

//Zuul过滤器都继承ZuulFilter，并且覆盖4个方法：
//filterType()
//filterOrder()
//shouldFilter()
//run()
@Component
public class TrackingFilter extends ZuulFilter {
	
	private static final int FILTER_ORDER = 1;
	private static final boolean SHOULD_FILTER = true;
	private static final Logger logger = LoggerFactory.getLogger(TrackingFilter.class);
	
	@Autowired
	FilterUtils filterUtils;
	
	//此方法告知Zuul，该过滤器是前置过滤器，路由过滤器，还是后置过滤器
	@Override
	public String filterType() {
		// TODO Auto-generated method stub
		return FilterUtils.PRE_FILTER_TYPE;
	}
	
	//返回的整形值表示不同类型的过滤器的执行顺序
	@Override
	public int filterOrder() {
		// TODO Auto-generated method stub
		return FILTER_ORDER;
	}
	
	//返回boolean表示是否需要过滤
	@Override
	public boolean shouldFilter() {
		// TODO Auto-generated method stub
		return SHOULD_FILTER;
	}
	
	private boolean isCorrelationIdPresent() {
		if(filterUtils.getCorrelationId() != null) {
			return true;
		}
		return false;
	}

	private String generateCorrelationId() {
		return java.util.UUID.randomUUID().toString();
	}
	
	//该方法检查filterUtils中是否存在一个tmx-correlation-id
	//其本质是检查RequestContext.getCurrentContext()中是否包含tmx-correlation-id
	//如果存在则放过他，若不存在则生成一个filter
	@Override
	public Object run() throws ZuulException {
		if(isCorrelationIdPresent()) {
			logger.debug("tmx-correlation-id found in tracking filter:{}.",filterUtils.getCorrelationId());
		}else {
			filterUtils.setCorrelationId(generateCorrelationId());
			logger.debug("tmx-correlation-id generated in tracking filter:{}.",filterUtils.getCorrelationId());
		}
		RequestContext ctx = RequestContext.getCurrentContext();
		logger.debug("Processing incoming request for{}.",ctx.getRequest().getRequestURI());
		return null;
	}
}
