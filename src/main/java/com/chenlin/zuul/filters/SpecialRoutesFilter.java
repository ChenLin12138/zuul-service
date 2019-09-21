package com.chenlin.zuul.filters;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.chenlin.zuul.filters.model.AbTestingRoute;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

/**
 * @author Chen Lin
 * @date 2019-09-17
 */

@Component
public class SpecialRoutesFilter extends ZuulFilter {

	private static final int FILTER_ORDER = 1;
	private static final boolean SHOULD_FILTER = true;

	@Autowired
	FilterUtils filterUtils;

	@Autowired
	RestTemplate restTemplate;

	@Override
	public String filterType() {
		// TODO Auto-generated method stub
		return FilterUtils.ROUTE_FILTER_TYPE;
	}

	@Override
	public int filterOrder() {
		// TODO Auto-generated method stub
		return FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		// TODO Auto-generated method stub
		return SHOULD_FILTER;
	}

	//helper变量是类ProxyRequestHelper类型的一个实例变量，这是Spring Cloud提供的类
	//带有用于代理服务请求的辅助方法
	private ProxyRequestHelper helper = new ProxyRequestHelper();

	private AbTestingRoute getAbRoutingInfo(String serviceName) {
		ResponseEntity<AbTestingRoute> restExchange = null;
		try {
			// 调用specialRouteService的端点
			restExchange = restTemplate.exchange("http://specialroutesservice/v1/route/abtesting/{serviceId}",
					HttpMethod.GET, null, AbTestingRoute.class, serviceName);
		} // 如果路由服务没有找到记录，则返回404
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND)
				return null;
			throw ex;
		}

		return restExchange.getBody();
	}

	private String buildRouteString(String oldEndpoint, Object newEndpoint, String serviceName) {
		int index = oldEndpoint.indexOf(serviceName);
		String strippedRoute = oldEndpoint.substring(index + serviceName.length());
		System.out.println("Target route:" + String.format("%s/%s", newEndpoint, strippedRoute));
		return String.format("%s/%s", newEndpoint, strippedRoute);
	}

	private String getVerb(HttpServletRequest request) {
		String sMethod = request.getMethod();
		return sMethod.toUpperCase();
	}

	private HttpHost getHttpHost(URL host) {
		HttpHost httpHost = new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
		return httpHost;
	}

	private Header[] convertHeaders(MultiValueMap<String, String> headers) {
		List<Header> list = new ArrayList<Header>();
		for (String name : headers.keySet()) {
			for (String value : headers.get(name)) {
				list.add(new BasicHeader(name, value));
			}
		}
		return list.toArray(new BasicHeader[0]);
	}

	private HttpResponse forwardRequest(HttpClient httpclient, HttpHost httpHost, HttpRequest httpRequest)
			throws IOException {
		return httpclient.execute(httpHost, httpRequest);
	}

	private MultiValueMap<String, String> revertHeaders(Header[] headers) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		for (Header header : headers) {
			String name = header.getName();
			if (!map.containsKey(name)) {
				map.put(name, new ArrayList<String>());
			}
			map.get(name).add(header.getValue());
		}
		return map;
	}

	private InputStream getRequestBody(HttpServletRequest request) {
		InputStream requestEntity = null;
		try {
			requestEntity = request.getInputStream();
		} catch (IOException ex) {

		}
		return requestEntity;
	}
	
	private void setResponse(HttpResponse response) throws IOException{
		this.helper.setResponse(response.getStatusLine().getStatusCode(), response.getEntity() == null ? null: response.getEntity().getContent(), revertHeaders(response.getAllHeaders()));
	}

	private HttpResponse forward(HttpClient httpClient, String verb, String uri, HttpServletRequest request,
			MultiValueMap<String, String> headers, MultiValueMap<String, String> params, InputStream requestEntity)
			throws Exception {
		Map<String, Object> info = this.helper.debug(verb, uri, headers, params, requestEntity);
		URL host = new URL(uri);
		HttpHost httpHost = getHttpHost(host);
		HttpRequest httpRequest;
		int contentLength = request.getContentLength();
		InputStreamEntity entity = new InputStreamEntity(requestEntity, contentLength,
				request.getContentType() != null ? ContentType.create(request.getContentType()) : null);
		switch (verb.toUpperCase()) {
		case "POST":
			HttpPost httpPost = new HttpPost(uri);
			httpRequest = httpPost;
			httpPost.setEntity(entity);
			break;
		case "PUT":
			HttpPut httpPut = new HttpPut(uri);
			httpRequest = httpPut;
			httpPut.setEntity(entity);
			break;
		case "PATCH":
			HttpPatch httpPatch = new HttpPatch(uri);
			httpRequest = httpPatch;
			httpPatch.setEntity(entity);
			break;
		default:
			httpRequest = new BasicHttpRequest(verb,uri);
		}
		try {
			httpRequest.setHeaders(convertHeaders(headers));
			HttpResponse zuulResponse = forwardRequest(httpClient, httpHost, httpRequest);
			return zuulResponse;
		}finally {
			
		}
	}
	
	private boolean useSpecialRoute(AbTestingRoute abTestRoute) {
		Random random = new Random();
		//检查路由是否为活跃状态
		if (abTestRoute.getActive().equals("N"))
			return false;

		//确定是否应该使用替代服务路由
		int value = random.nextInt((10 - 1) + 1) + 1;

		if (abTestRoute.getWeight() < value)
			return true;

		return false;
	}

	@Override
	public Object run() throws ZuulException {
		RequestContext ctx = RequestContext.getCurrentContext();

		//执行对SpecialRoutes服务的调用，以确定该服务id是否有路由记录
		AbTestingRoute abTestRoute = getAbRoutingInfo(filterUtils.getServiceId());

		// userSpecialRoute将接受路径的权重，生成一个随机数，并确定是否将请求转发到替代服务
		if (null != abTestRoute && useSpecialRoute(abTestRoute)) {
			// 若abTestRoute存在，则将完成的url构建到由specialroutes服务指定的服务位置。
			String route = buildRouteString(ctx.getRequest().getRequestURI(), abTestRoute.getEndpoint(),
					ctx.get("serviceId").toString());
			// 完成转发到其他服务的工作
			forwardToSpecialRoute(route);
		}

		return null;
	}

	private void forwardToSpecialRoute(String route) {
		RequestContext context = RequestContext.getCurrentContext();
		HttpServletRequest request = context.getRequest();
		//创建将发送到服务的所有http请求首部的副本
		MultiValueMap<String, String> headers = this.helper.buildZuulRequestHeaders(request);
		//创建所有http请求参数的副本
		MultiValueMap<String, String> params = this.helper.buildZuulRequestQueryParams(request);
		String verb = getVerb(request);
		//创建将被转发到替代服务的http主体的副本
		InputStream requestEntity = getRequestBody(request);

		if (request.getContentLength() < 0) {
			context.setChunkedRequestBody();
		}

		this.helper.addIgnoredHeaders();
		CloseableHttpClient httpClient = null;
		HttpResponse response = null;

		try {
			httpClient = HttpClients.createDefault();
			//使用forward()辅助方法调用替代服务
			response = forward(httpClient, verb, route, request, headers, params, requestEntity);
			//通过setResponse()辅助方法将服务调用的结果保存到zuul服务器
			setResponse(response);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				httpClient.close();
			} catch (IOException ex) {

			}
		}

	}
}
