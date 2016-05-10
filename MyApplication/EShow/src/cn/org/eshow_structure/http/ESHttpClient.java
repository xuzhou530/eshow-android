/*
 * Copyright (C) 2012 www.amsoft.cn
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.org.eshow_structure.http;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Message;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLHandshakeException;

import cn.org.eshow_structure.cache.ESCacheHeaderParser;
import cn.org.eshow_structure.cache.ESCacheResponse;
import cn.org.eshow_structure.cache.ESCacheUtil;
import cn.org.eshow_structure.cache.ESDiskBaseCache;
import cn.org.eshow_structure.cache.ESDiskCache.Entry;
import cn.org.eshow_structure.cache.http.ESHttpBaseCache;
import cn.org.eshow_structure.global.ESAppConfig;
import cn.org.eshow_structure.global.ESAppException;
import cn.org.eshow_structure.http.entity.MultipartEntity;
import cn.org.eshow_structure.http.ssl.EasySSLProtocolSocketFactory;
import cn.org.eshow_structure.image.ESImageLoader;
import cn.org.eshow_structure.task.thread.ESThreadFactory;
import cn.org.eshow_structure.util.ESAppUtil;
import cn.org.eshow_structure.util.ESFileUtil;
import cn.org.eshow_structure.util.ESLogUtil;

// TODO: Auto-generated Javadoc
/**
 * Http客户端
 *
 */
public class ESHttpClient {
	
    /** 上下文. */
	private static Context mContext;
	
	/** 线程执行器. */
	public static Executor mExecutorService = null;
	
	/** 编码. */
	private String encode = HTTP.UTF_8;
	
	/** 用户代理. */
	private String userAgent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.43 BIDUBrowser/6.x Safari/537.31";
	
    private static final String HTTP_GET = "GET";
    private static final String HTTP_POST = "POST";
    private static final String USER_AGENT = "User-Agent";
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    
    /** CookieStore. */
    private CookieStore mCookieStore;  
    
    /** 缓存超时时间设置. */
    private long cacheMaxAge;

    /** 最大连接数. */
    private static final int DEFAULT_MAX_CONNECTIONS = 10;
    
    /** 超时时间. */
    public static final int DEFAULT_SOCKET_TIMEOUT = 10000;
    
    /** 重试次数. */
    private static final int DEFAULT_MAX_RETRIES = 2;
    
    /** 缓冲大小. */
    private static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;
    
    /** 成功. */
    protected static final int SUCCESS_MESSAGE = 0;
    
    /** 失败. */
    protected static final int FAILURE_MESSAGE = 1;
    
    /** 和网络相关的失败. */
    protected static final int FAILURE_MESSAGE_CONNECT = 2;
    
    /** 和服务相关的失败. */
    protected static final int FAILURE_MESSAGE_SERVICE = 3;
    
    /** 开始. */
    protected static final int START_MESSAGE = 4;
    
    /** 完成. */
    protected static final int FINISH_MESSAGE = 5;
    
    /** 进行中. */
    protected static final int PROGRESS_MESSAGE = 6;
    
    /** 重试. */
    protected static final int RETRY_MESSAGE = 7;
    
    /** 超时时间. */
	private int mTimeout = DEFAULT_SOCKET_TIMEOUT;
	
	/** 通用证书. 如果要求HTTPS连接，则使用SSL打开连接*/
	private boolean mIsOpenEasySSL = true;
	
	/** HTTP Client*/
	private DefaultHttpClient mHttpClient = null;
	
	/** HTTP 上下文*/
	private HttpContext mHttpContext = null;
	
    /** HTTP缓存. */
    private ESHttpBaseCache httpCache;
	
	 /** 磁盘缓存. */
    private ESDiskBaseCache diskCache;
    
    /**
     * 初始化.
     *
     * @param context the context
     */
	public ESHttpClient(Context context) {
	    mContext = context;
		mExecutorService =  ESThreadFactory.getExecutorService();
		mHttpContext = new BasicHttpContext();
		cacheMaxAge = ESAppConfig.DISK_CACHE_EXPIRES_TIME;
    	PackageInfo info = ESAppUtil.getPackageInfo(context);
    	File cacheDir = null;
    	if(!ESFileUtil.isCanUseSD()){
    		cacheDir = new File(context.getCacheDir(), info.packageName);
		}else{
			cacheDir = new File(ESFileUtil.getCacheDownloadDir(context));
		}
    	this.httpCache = ESHttpBaseCache.getInstance();
    	this.diskCache = new ESDiskBaseCache(cacheDir);
	}
	

	/**
	 * 描述：带参数的get请求.
	 *
	 * @param url the url
	 * @param params the params
	 * @param responseListener the response listener
	 */
	public void get(final String url,final ESRequestParams params,final ESHttpResponseListener responseListener) {
		
		responseListener.setHandler(new ResponderHandler(responseListener));
		mExecutorService.execute(new Runnable() { 
    		public void run() {
    			try {
    				//放这里的目的是保证只执行一次，避免重定向后重复执行
    				responseListener.sendStartMessage();
    				doGet(url, params, responseListener);
    			} catch (Exception e) { 
    				e.printStackTrace();
    			}
    		}                 
    	});      
	}
	
	/**
	 * 描述：带参数的get请求(有缓存).
	 *
	 * @param url the url
	 * @param params the params
	 * @param responseListener the response listener
	 */
	public void getWithCache(final String url,final ESRequestParams params,final ESHttpResponseListener responseListener) {
		
		responseListener.setHandler(new ResponderHandler(responseListener));
		mExecutorService.execute(new Runnable() { 
    		public void run() {
    			try {
    				responseListener.sendStartMessage();
    				
    				String httpUrl = url;
    				//HttpGet连接对象  
				    if(params!=null){
					  if(url.indexOf("?")==-1){
						  httpUrl += "?";
					  }
					  httpUrl += params.getParamString();
				    }
    				
    				//查看本地缓存
    				final String cacheKey = httpCache.getCacheKey(httpUrl);
    				//看磁盘
    				Entry entry = diskCache.get(cacheKey);
    				if(entry == null || entry.isExpired()){
    					ESLogUtil.i(ESImageLoader.class, "磁盘中没有缓存，或者已经过期");
    					
    					if(!ESAppUtil.isNetworkAvailable(mContext)){
    					    Thread.sleep(200);
    						responseListener.sendFailureMessage(ESHttpStatus.CONNECT_FAILURE_CODE, ESAppConfig.CONNECT_EXCEPTION, new ESAppException(ESAppConfig.CONNECT_EXCEPTION));
    				        return;
    				    }
    					
    					ESCacheResponse response = ESCacheUtil.getCacheResponse(httpUrl);
    					
    					if(response!=null){
    						String responseBody = new String(response.data);
    						ESLogUtil.i(mContext, "1[HTTP GET]:" + httpUrl + ",result：" + responseBody);
    						Entry entryNew = ESCacheHeaderParser.parseCacheHeaders(response, cacheMaxAge);
    						if(entryNew!=null){
    							diskCache.put(cacheKey,entryNew);
    							ESLogUtil.i(mContext, "HTTP 缓存成功");
    						}else{
    							ESLogUtil.i(mContext, "HTTP 缓存失败，parseCacheHeaders失败");
    						}
    						((ESStringHttpResponseListener)responseListener).sendSuccessMessage(ESHttpStatus.SUCCESS_CODE, responseBody);
    					}else{
    						responseListener.sendFailureMessage(ESHttpStatus.SERVER_FAILURE_CODE, ESAppConfig.REMOTE_SERVICE_EXCEPTION,new ESAppException(ESAppConfig.REMOTE_SERVICE_EXCEPTION));
    					}
    				}else{
    					Thread.sleep(200);
    					//磁盘中有
    					byte [] httpData = entry.data;
	      	            String responseBody = new String(httpData);
	      	            ((ESStringHttpResponseListener)responseListener).sendSuccessMessage(ESHttpStatus.SUCCESS_CODE, responseBody);
	      	            ESLogUtil.i(mContext, "[HTTP GET CACHED]:" + httpUrl + ",result：" + responseBody);
    				}
    				
    			}catch (Exception e) {
    				e.printStackTrace();
    				//发送失败消息
    				responseListener.sendFailureMessage(ESHttpStatus.UNTREATED_CODE, e.getMessage(), new ESAppException(e));
    			}finally{
    				responseListener.sendFinishMessage();
    			}
    		}                 
    	});      
	}
	
	/**
	 * 描述：带参数的post请求.
	 *
	 * @param url the url
	 * @param params the params
	 * @param responseListener the response listener
	 */
	public void post(final String url,final ESRequestParams params, final ESHttpResponseListener responseListener) {
		responseListener.setHandler(new ResponderHandler(responseListener));
		mExecutorService.execute(

				new Runnable() {

					public void run() {
						try {
							responseListener.sendStartMessage();
							doPost(url,params,responseListener);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
		);
	}
	
	
	/**
	 * 描述：执行get请求.
	 *
	 * @param url the url
	 * @param params the params
	 * @param responseListener the response listener
	 */
	private void doGet(String url,ESRequestParams params,ESHttpResponseListener responseListener){
		  try {
			  
			  if(!ESAppUtil.isNetworkAvailable(mContext)){
				    Thread.sleep(200);
					responseListener.sendFailureMessage(ESHttpStatus.CONNECT_FAILURE_CODE, ESAppConfig.CONNECT_EXCEPTION, new ESAppException(ESAppConfig.CONNECT_EXCEPTION));
			        return;
			  }
			  
			  //HttpGet连接对象  
			  if(params!=null){
				  if(url.indexOf("?")==-1){
					  url += "?";
				  }
				  url += params.getParamString();
			  }
			  HttpGet httpGet = new HttpGet(url);  
			  httpGet.addHeader(USER_AGENT, userAgent);
			  //压缩
			  httpGet.addHeader(ACCEPT_ENCODING, "gzip");
			  //取得默认的HttpClient
      	      HttpClient httpClient = getHttpClient();  
		      //取得HttpResponse
		      httpClient.execute(httpGet,new RedirectionResponseHandler(url,responseListener),mHttpContext);  
		} catch (Exception e) {
			e.printStackTrace();
			//发送失败消息
			responseListener.sendFailureMessage(ESHttpStatus.UNTREATED_CODE,e.getMessage(),new ESAppException(e));
		}
	}
	
	/**
	 * 描述：执行post请求.
	 *
	 * @param url the url
	 * @param params the params
	 * @param responseListener the response listener
	 */
	private void doPost(String url,ESRequestParams params,ESHttpResponseListener responseListener){
		  try {
			  
			  if(!ESAppUtil.isNetworkAvailable(mContext)){
				    Thread.sleep(200);
					responseListener.sendFailureMessage(ESHttpStatus.CONNECT_FAILURE_CODE, ESAppConfig.CONNECT_EXCEPTION, new ESAppException(ESAppConfig.CONNECT_EXCEPTION));
			        return;
			  }
			  
			  //HttpPost连接对象  
		      HttpPost httpPost = new HttpPost(url);  
		      httpPost.addHeader(USER_AGENT, userAgent);
			  //压缩
		      httpPost.addHeader(ACCEPT_ENCODING, "gzip");
		      //是否包含文件
		      boolean isContainFile = false;
		      if(params != null){
		    	  //使用NameValuePair来保存要传递的Post参数设置字符集 
			      HttpEntity httpentity = params.getEntity();
			      //请求httpRequest  
			      httpPost.setEntity(httpentity); 
			      if(params.getFileParams().size()>0){
			    	  isContainFile = true;
			      }
			  }
		      //取得默认的HttpClient
		      DefaultHttpClient httpClient = getHttpClient();  
		      if(isContainFile){
		    	  httpPost.addHeader("connection", "keep-alive");
			      httpPost.addHeader("Content-Type", "multipart/form-data; boundary=" + params.boundaryString());
		    	  ESLogUtil.i(mContext, "[HTTP POST]:" + url + ",包含文件域!");
		      }else {
				  ESLogUtil.i(mContext, "isContainFile is "+String.valueOf(isContainFile));
			  }
		      //取得HttpResponse
		      httpClient.execute(
                      httpPost,
                      new RedirectionResponseHandler(url,responseListener),
                      mHttpContext);
			  
		} catch (Exception e) {
			e.printStackTrace();
			ESLogUtil.i(mContext, "[HTTP POST]:" + url + ",error：" + e.getMessage());
			//发送失败消息
			responseListener.sendFailureMessage(ESHttpStatus.UNTREATED_CODE,e.getMessage(),new ESAppException(e));
		}
	}
	
	/**
     * 简单的请求,只支持返回的数据是String类型,不支持转发重定向
     * @param url
     * @param params
     * @return
     */
    public void doRequest(final String url, final ESRequestParams params, final ESStringHttpResponseListener responseListener) {
    	responseListener.setHandler(new ResponderHandler(responseListener));
		mExecutorService.execute(new Runnable() { 
    		public void run() {
    			HttpURLConnection urlConn = null;
    			try {
    				responseListener.sendStartMessage();
        			
        			if(!ESAppUtil.isNetworkAvailable(mContext)){
        				Thread.sleep(200);
    					responseListener.sendFailureMessage(ESHttpStatus.CONNECT_FAILURE_CODE, ESAppConfig.CONNECT_EXCEPTION, new ESAppException(ESAppConfig.CONNECT_EXCEPTION));
    			        return;
    			    }
        			
    				String resultString = null;
					URL requestUrl = new URL(url);
					urlConn = (HttpURLConnection) requestUrl.openConnection();
			        urlConn.setRequestMethod("POST");
			        urlConn.setConnectTimeout(mTimeout);
			        urlConn.setReadTimeout(mTimeout);
			        urlConn.setDoOutput(true);
			        
					if(params!=null){
						urlConn.setRequestProperty("connection", "keep-alive");
				        urlConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + params.boundaryString());
						MultipartEntity reqEntity = params.getMultiPart();
				        reqEntity.writeTo(urlConn.getOutputStream());
					}else{
						urlConn.connect();
					}
			       
		            if (urlConn.getResponseCode() == HttpStatus.SC_OK){
		            	resultString = readString(urlConn.getInputStream());
		            }else{
		            	resultString = readString(urlConn.getErrorStream());
		            }
		            resultString = URLEncoder.encode(resultString, encode);
		            urlConn.getInputStream().close();
		            responseListener.sendSuccessMessage(ESHttpStatus.SUCCESS_CODE, resultString);
    			} catch (Exception e) { 
    				e.printStackTrace();
					ESLogUtil.i(mContext, "[HTTP POST]:" + url + ",error：" + e.getMessage());
					//发送失败消息
					responseListener.sendFailureMessage(ESHttpStatus.UNTREATED_CODE,e.getMessage(),new ESAppException(e));
    			} finally {
					if (urlConn != null)
						urlConn.disconnect();
					
					responseListener.sendFinishMessage();
				}
    		}                 
    	});      
	}
	
	/**
	 * 描述：写入文件并回调进度.
	 * 
	 * @param context the context
	 * @param entity the entity
	 * @param name the name
	 * @param responseListener the response listener
	 */
    public void writeResponseData(Context context,HttpEntity entity,String name,ESFileHttpResponseListener responseListener){
        
    	if(entity == null){
        	return;
        }
    	
    	if(responseListener.getFile() == null){
    		//创建缓存文件
    		responseListener.setFile(context,name);
        }
    	
    	InputStream inStream = null;
    	FileOutputStream outStream = null;
        try {
	        inStream = entity.getContent();
	        long contentLength = entity.getContentLength();
	        outStream = new FileOutputStream(responseListener.getFile());
	        if (inStream != null) {
	          
	              byte[] tmp = new byte[4096];
	              int l, count = 0;
	              while ((l = inStream.read(tmp)) != -1 && !Thread.currentThread().isInterrupted()) {
	                  count += l;
	                  outStream.write(tmp, 0, l);
	                  responseListener.sendProgressMessage(count, (int) contentLength);
	              }
	        }
	        responseListener.sendSuccessMessage(200);
	    }catch(Exception e){
	        e.printStackTrace();
	        //发送失败消息
			responseListener.sendFailureMessage(ESHttpStatus.RESPONSE_TIMEOUT_CODE, ESAppConfig.SOCKET_TIMEOUT_EXCEPTION,e);
	    } finally {
        	try {
        		if(inStream!=null){
        			inStream.close();
        		}
        		if(outStream!=null){
        			outStream.flush();
    				outStream.close();
        		}
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        
    }
    
    /**
     * 描述：转换为二进制并回调进度.
     *
     * @param entity the entity
     * @param responseListener the response listener
     */
    public void readResponseData(HttpEntity entity,ESBinaryHttpResponseListener responseListener){
        
    	if(entity == null){
        	return;
        }
    	
        InputStream inStream = null;
        ByteArrayOutputStream outSteam = null; 
       
    	try {
    		inStream = entity.getContent();
			outSteam = new ByteArrayOutputStream(); 
			long contentLength = entity.getContentLength();
			if (inStream != null) {
				  int l, count = 0;
				  byte[] tmp = new byte[4096];
				  while((l = inStream.read(tmp))!=-1){ 
					  count += l;
			          outSteam.write(tmp,0,l); 
			          responseListener.sendProgressMessage(count, (int) contentLength);

			     } 
			  }
			 responseListener.sendSuccessMessage(HttpStatus.SC_OK,outSteam.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
			//发送失败消息
			responseListener.sendFailureMessage(ESHttpStatus.RESPONSE_TIMEOUT_CODE, ESAppConfig.SOCKET_TIMEOUT_EXCEPTION,e);
		}finally{
			try {
				if(inStream!=null){
					inStream.close();
				}
				if(outSteam!=null){
					outSteam.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    	
        
    }
    
    /**
     * 描述：设置连接超时时间.
     *
     * @param timeout 毫秒
     */
    public void setTimeout(int timeout) {
    	this.mTimeout = timeout;
	}

	/**
	 * © 2012 amsoft.cn
	 * 名称：ResponderHandler.java 
	 * 描述：请求返回
	 *

	 * @version v1.0
	 * @date：2013-11-13 下午3:22:30
	 */
    private static class ResponderHandler extends Handler {
		
		/** 响应数据. */
		private Object[] response;
		
		/** 响应消息监听. */
		private ESHttpResponseListener responseListener;
		

		/**
		 * 响应消息处理.
		 *
		 * @param responseListener the response listener
		 */
		public ResponderHandler(ESHttpResponseListener responseListener) {
			this.responseListener = responseListener;
		}

		/* (non-Javadoc)
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			
			case SUCCESS_MESSAGE:
				response = (Object[]) msg.obj;
				
				if (response != null){
					if(responseListener instanceof ESStringHttpResponseListener){
						if(response.length >= 2){
							((ESStringHttpResponseListener)responseListener).onSuccess((Integer) response[0],(String)response[1]);
						}else{
							ESLogUtil.e(mContext, "SUCCESS_MESSAGE " + ESAppConfig.MISSING_PARAMETERS);
						}
						
					}else if(responseListener instanceof ESBinaryHttpResponseListener){
						if(response.length >= 2){ 
						    ((ESBinaryHttpResponseListener)responseListener).onSuccess((Integer) response[0],(byte[])response[1]);
						}else{
							ESLogUtil.e(mContext, "SUCCESS_MESSAGE " + ESAppConfig.MISSING_PARAMETERS);
						}
					}else if(responseListener instanceof ESFileHttpResponseListener){
						
						if(response.length >= 1){ 
							ESFileHttpResponseListener mAbFileHttpResponseListener = ((ESFileHttpResponseListener)responseListener);
							mAbFileHttpResponseListener.onSuccess((Integer) response[0],mAbFileHttpResponseListener.getFile());
						}else{
							ESLogUtil.e(mContext, "SUCCESS_MESSAGE " + ESAppConfig.MISSING_PARAMETERS);
						}
						
					}
				}
                break;
			case FAILURE_MESSAGE:
				 response = (Object[]) msg.obj;
                 if (response != null && response.length >= 3){
                	 //异常转换为可提示的
                	 ESAppException exception = new ESAppException((Exception) response[2]);
					 responseListener.onFailure((Integer) response[0], (String) response[1], exception);
                 }else{
                	 ESLogUtil.e(mContext, "FAILURE_MESSAGE " + ESAppConfig.MISSING_PARAMETERS);
                 }
	             break;
			case START_MESSAGE:
				responseListener.onStart();
				break;
			case FINISH_MESSAGE:
				responseListener.onFinish();
				break;
			case PROGRESS_MESSAGE:
				 response = (Object[]) msg.obj;
	             if (response != null && response.length >= 2){
	            	 responseListener.onProgress((Long) response[0], (Long) response[1]);
			     }else{
			    	 ESLogUtil.e(mContext, "PROGRESS_MESSAGE " + ESAppConfig.MISSING_PARAMETERS);
			     }
	             break;
			case RETRY_MESSAGE:
				responseListener.onRetry();
				break;
			default:
				break;
		   }
		}
		
	}
    
    /**
     * HTTP参数配置
     * @return
     */
    public BasicHttpParams getHttpParams(){
    	
    	BasicHttpParams httpParams = new BasicHttpParams();
        
        // 设置每个路由最大连接数
        ConnPerRouteBean connPerRoute = new ConnPerRouteBean(30);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams, connPerRoute);
        HttpConnectionParams.setStaleCheckingEnabled(httpParams, false);
		// 从连接池中取连接的超时时间，设置为1秒
	    ConnManagerParams.setTimeout(httpParams, mTimeout);
	    ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(DEFAULT_MAX_CONNECTIONS));
	    ConnManagerParams.setMaxTotalConnections(httpParams, DEFAULT_MAX_CONNECTIONS);
	    // 读响应数据的超时时间
	    HttpConnectionParams.setSoTimeout(httpParams, mTimeout);
	    HttpConnectionParams.setConnectionTimeout(httpParams, mTimeout);
	    HttpConnectionParams.setTcpNoDelay(httpParams, true);
	    HttpConnectionParams.setSocketBufferSize(httpParams, DEFAULT_SOCKET_BUFFER_SIZE);

	    HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
	    HttpProtocolParams.setUserAgent(httpParams,userAgent);
	    //默认参数
        HttpClientParams.setRedirecting(httpParams, false);
        HttpClientParams.setCookiePolicy(httpParams,
                CookiePolicy.BROWSER_COMPATIBILITY);
        httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, null);
		return httpParams;
	      
    }
    
    /**
     * 获取HttpClient，自签名的证书，如果想做签名参考AuthSSLProtocolSocketFactory类
     * @return
     */
    public DefaultHttpClient getHttpClient(){
    	
    	if(mHttpClient != null){
    		return mHttpClient;
    	}else{
    		return createHttpClient();
    	}
    }
    
    /**
     * 获取HttpClient，自签名的证书，如果想做签名参考AuthSSLProtocolSocketFactory类
     * @return
     */
    public DefaultHttpClient createHttpClient(){
    	BasicHttpParams httpParams = getHttpParams();
    	if(mIsOpenEasySSL){
    		 // 支持https的   SSL自签名的实现类
    	     EasySSLProtocolSocketFactory easySSLProtocolSocketFactory = new EasySSLProtocolSocketFactory();
             SchemeRegistry supportedSchemes = new SchemeRegistry();
             SocketFactory socketFactory = PlainSocketFactory.getSocketFactory();
             supportedSchemes.register(new Scheme("http", socketFactory, 80));
             supportedSchemes.register(new Scheme("https",easySSLProtocolSocketFactory, 443));
             //安全的ThreadSafeClientConnManager，否则不能用单例的HttpClient
             ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(
                     httpParams, supportedSchemes);
             //取得HttpClient ThreadSafeClientConnManager
             mHttpClient = new DefaultHttpClient(connectionManager, httpParams);
    	}else{
    		 //线程安全的HttpClient
    		 mHttpClient = new DefaultHttpClient(httpParams);
    	}
    	//自动重试
    	mHttpClient.setHttpRequestRetryHandler(mRequestRetryHandler);
    	mHttpClient.setCookieStore(mCookieStore);
 	    return mHttpClient;
    }

    /**
     * 是否打开ssl 自签名
     */
    public boolean isOpenEasySSL(){
        return mIsOpenEasySSL;
    }


    /**
     * 打开ssl 自签名
     * @param isOpenEasySSL
     */
    public void setOpenEasySSL(boolean isOpenEasySSL){
        this.mIsOpenEasySSL = isOpenEasySSL;
    }
    
    /**
     * 使用ResponseHandler接口处理响应,支持重定向
     */
    private class RedirectionResponseHandler implements ResponseHandler<String>{
        
    	private ESHttpResponseListener mResponseListener = null;
    	private String mUrl = null;
    	
		public RedirectionResponseHandler(String url, ESHttpResponseListener responseListener) {
			super();
			this.mUrl = url;
			this.mResponseListener = responseListener;
		}


		@Override
        public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException{
            HttpUriRequest request = (HttpUriRequest) mHttpContext.getAttribute(ExecutionContext.HTTP_REQUEST);
            //请求成功  
  			int statusCode = response.getStatusLine().getStatusCode();
  			HttpEntity entity = response.getEntity();
  			String responseBody = null;

            if (statusCode == HttpStatus.SC_OK) {
                //200直接返回结果

                // 不打算读取response body
                // 调用request的abort方法  
                // request.abort();
                if (entity != null){
	  				  if(mResponseListener instanceof ESStringHttpResponseListener){
	  					  //entity中的内容只能读取一次,否则Content has been consumed
	  					  //如果压缩要解压
	                      Header header = entity.getContentEncoding();
	                      if (header != null){
	                          String contentEncoding = header.getValue();
	                          if (contentEncoding != null){
	                              if (contentEncoding.contains("gzip")){
	                                  entity = new ESGzipDecompressingEntity(entity);
	                              }
	                          }
	                      }
	                      String charset = EntityUtils.getContentCharSet(entity) == null ? encode : EntityUtils.getContentCharSet(entity);
	      	              responseBody = new String(EntityUtils.toByteArray(entity), charset);
	      	              
	      	              ESLogUtil.i(mContext, "2[HTTP GET]:" + request.getURI() + ",result：" + responseBody);
                          try {
                              JSONObject ret=new JSONObject(responseBody);
                              int rr=ret.getInt("status");
                              ESLogUtil.i(mContext,"status =="+rr);
                              if(rr==-5){
                                  if("/user/login.json".equals(request.getURI().toString())){
                                      ((ESStringHttpResponseListener) mResponseListener).sendFailureMessage(statusCode, responseBody, new ESAppException(ret.getString("msg")));
                                  }else if("/user/check.json".equals(request.getURI().toString())) {
                                      ((ESStringHttpResponseListener) mResponseListener).sendFailureMessage(statusCode, responseBody, new ESAppException(ESAppConfig.ACCOUNT_ISEXIST));
                                  }
                              }else if(rr==-9){
                                  ((ESStringHttpResponseListener) mResponseListener).sendFailureMessage(statusCode, responseBody, new ESAppException(ESAppConfig.LOG_MISS_EXCEPTION));
                              }else if(rr==1){
                                  ((ESStringHttpResponseListener) mResponseListener).sendSuccessMessage(statusCode, responseBody);
                              }else {
                                  ((ESStringHttpResponseListener) mResponseListener).sendFailureMessage(statusCode, responseBody,null);
                              }
                          } catch (JSONException e) {
                              e.printStackTrace();
                              ((ESStringHttpResponseListener) mResponseListener).sendFailureMessage(statusCode, responseBody, e);
                          }
	  				  }else if(mResponseListener instanceof ESBinaryHttpResponseListener){
	  					  responseBody = "Binary";
	  					  ESLogUtil.i(mContext, "3[HTTP GET]:" + request.getURI() + ",result：" + responseBody);
	  					  readResponseData(entity,((ESBinaryHttpResponseListener)mResponseListener));
	  				  }else if(mResponseListener instanceof ESFileHttpResponseListener){
	  					  //获取文件名
	  					  String fileName = ESFileUtil.getCacheFileNameFromUrl(mUrl, response);
	  					  ESLogUtil.i(mContext, "4[HTTP GET]:" + request.getURI() + ",result：" + fileName);
	  					  writeResponseData(mContext,entity,fileName,((ESFileHttpResponseListener)mResponseListener));
	  				  }
	      		      //资源释放!!!
	            	  try {
						  entity.consumeContent();
					  } catch (Exception e) {
						  e.printStackTrace();
					  }
	            	  
	            	  //完成消息
	                  mResponseListener.sendFinishMessage();
	      			  return responseBody;
                }
                
            } else if (statusCode == HttpStatus.SC_MOVED_TEMPORARILY || statusCode == HttpStatus.SC_MOVED_PERMANENTLY){
				//301 302进行重定向请求

                // 从头中取出转向的地址
                Header locationHeader = response.getLastHeader("location");
                String location = locationHeader.getValue();
                if (request.getMethod().equalsIgnoreCase(HTTP_POST)){
                    doPost(location, null, mResponseListener);
                }
                else if (request.getMethod().equalsIgnoreCase(HTTP_GET)){
                    doGet(location, null, mResponseListener);
                }
            }else if(statusCode == HttpStatus.SC_NOT_FOUND){
            	//404
            	mResponseListener.sendFailureMessage(statusCode, ESAppConfig.NOT_FOUND_EXCEPTION, new ESAppException(ESAppConfig.NOT_FOUND_EXCEPTION));
            	//完成消息
                mResponseListener.sendFinishMessage();
            }else if(statusCode == HttpStatus.SC_FORBIDDEN){
            	//403
            	mResponseListener.sendFailureMessage(statusCode, ESAppConfig.FORBIDDEN_EXCEPTION, new ESAppException(ESAppConfig.FORBIDDEN_EXCEPTION));
            	//完成消息
                mResponseListener.sendFinishMessage();
            }else{
                ESLogUtil.i(mContext,"other statusCode = "+statusCode);
            	//完成消息
                mResponseListener.sendFinishMessage();
  				mResponseListener.sendFailureMessage(statusCode, ESAppConfig.REMOTE_SERVICE_EXCEPTION, new ESAppException(ESAppConfig.REMOTE_SERVICE_EXCEPTION));
            }
            
            return null;
        }
    }
    
    /**
     * 自动重试处理
     */
    private HttpRequestRetryHandler mRequestRetryHandler = new HttpRequestRetryHandler(){
        
    	// 自定义的恢复策略
        public boolean retryRequest(IOException exception, int executionCount,
                HttpContext context){
            // 设置恢复策略，在发生异常时候将自动重试DEFAULT_MAX_RETRIES次
            if (executionCount >= DEFAULT_MAX_RETRIES){
                // 如果超过最大重试次数，那么就不要继续了
            	ESLogUtil.d(mContext, "超过最大重试次数，不重试");
                return false;
            }
            if (exception instanceof NoHttpResponseException){
                // 如果服务器丢掉了连接，那么就重试
            	ESLogUtil.d(mContext, "服务器丢掉了连接，重试");
                return true;
            }
            if (exception instanceof SSLHandshakeException){
                // SSL握手异常，不重试
            	ESLogUtil.d(mContext, "ssl 异常 不重试");
                return false;
            }
            HttpRequest request = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
            boolean idempotent = (request instanceof HttpEntityEnclosingRequest);
            if (!idempotent){
            	// 如果请求被认为是幂等的，那么就重试
            	ESLogUtil.d(mContext, "请求被认为是幂等的，重试");
                return true;
            }
            if (exception != null){
                return true;
            }
            return false;
        }
    };
    
	private String readString(InputStream is) {
		StringBuffer rst = new StringBuffer();
		
		byte[] buffer = new byte[1048576];
		int len = 0;
		
		try {
			while ((len = is.read(buffer)) > 0)
				for (int i = 0; i < len; ++i)
					rst.append((char)buffer[i]);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return rst.toString();
	}


    /**
     * 获取用户代理
     * @return
     */
    public String getUserAgent(){
        return userAgent;
    }


    /**
     * 设置用户代理
     * @param userAgent
     */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	
	
	/**
     * 获取编码
     * @return
     */
	public String getEncode() {
		return encode;
	}

	/**
	 * 设置编码
	 * @param encode
	 */
	public void setEncode(String encode) {
		this.encode = encode;
	}


	/**
	 * 关闭HttpClient
	 */
	public void shutdown(){
	    if(mHttpClient != null && mHttpClient.getConnectionManager() != null){
	    	mHttpClient.getConnectionManager().shutdown();
	    }
	}


	public CookieStore getCookieStore() {
		if(mHttpClient!=null){
			mCookieStore = mHttpClient.getCookieStore();
		}
		return mCookieStore;
	}


	public void setCookieStore(CookieStore cookieStore) {
		this.mCookieStore = cookieStore;
	}
	
	/**
	 * 
	 * 设置缓存的最大时间.
	 * @return
	 */
	public long getCacheMaxAge() {
		return cacheMaxAge;
	}


	/**
	 * 
	 * 获取缓存的最大时间.
	 * @param cacheMaxAge
	 */
	public void setCacheMaxAge(long cacheMaxAge) {
		this.cacheMaxAge = cacheMaxAge;
	}
	
}