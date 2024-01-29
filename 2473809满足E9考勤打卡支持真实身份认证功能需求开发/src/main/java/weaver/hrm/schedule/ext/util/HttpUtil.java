package weaver.hrm.schedule.ext.util;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import weaver.general.BaseBean;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Map;

public class HttpUtil {

    private static BaseBean log = new BaseBean() ;

    private static void writeLog(Object o){
        log.writeLog(HttpUtil.class.getName(),o);
    }

    public static String doGet(String url)throws Exception {
        HttpClient httpClient = FWHttpConnectionManager.getHttpClient();
        HttpClientParams params = httpClient.getParams() ;
        params.setContentCharset("utf-8");
        params.setHttpElementCharset("utf-8");

        GetMethod get = new GetMethod(url) ;
        int status = httpClient.executeMethod(get) ;
        InputStream inputStream = get.getResponseBodyAsStream() ;

        String json= IOUtils.toString(inputStream,"utf-8") ;

        writeLog("get url>>"+url+";;status>>>"+status+";;;json>>>>"+json) ;

        get.releaseConnection();

        return json ;
    }

    public static String doPost(String url, Map<String,String> requestParams)throws Exception {
        return doPost(url,requestParams,"utf-8") ;
    }



    public static String doPost(String url, Map<String,String> requestParams,String responeCharSet)throws Exception{
        return doPost(url,requestParams,null,responeCharSet) ;
    }

    public static String doPost(String url, Map<String,String> requestParams,Map<String,String> headers,String responeCharSet)throws Exception{

        HttpClient httpClient = FWHttpConnectionManager.getHttpClient();
        HttpClientParams params = httpClient.getParams() ;
        params.setContentCharset("utf-8");
        params.setHttpElementCharset("utf-8");

        PostMethod post = new PostMethod(url);

        if(headers != null){
            for(Map.Entry<String,String> entry : headers.entrySet()){
                post.addRequestHeader(entry.getKey(),entry.getValue());
            }
        }


        for(Map.Entry<String,String> entry : requestParams.entrySet()){
            post.addParameter(entry.getKey(),entry.getValue());
        }
        int status = httpClient.executeMethod(post) ;
        InputStream inputStream = post.getResponseBodyAsStream() ;
        String json = IOUtils.toString(inputStream,responeCharSet) ;
        post.releaseConnection();
        return json ;
    }

    public static String doPostForJson(String url, String param,Map<String,String> headers) throws Exception{
        String result = "";
        HttpClient httpClient = FWHttpConnectionManager.getHttpClient();
        CloseableHttpResponse response = null;
        PostMethod httppost = new PostMethod(url);
        HttpClientParams params = httpClient.getParams() ;
        params.setContentCharset("utf-8");
        params.setHttpElementCharset("utf-8");

        if(headers != null){
            for(Map.Entry<String,String> entry : headers.entrySet()){
                httppost.addRequestHeader(entry.getKey(),entry.getValue());
            }
        }

        RequestEntity jsonEntity = new StringRequestEntity(param, "application/json", "UTF-8");
        httppost.setRequestEntity(jsonEntity);

        httppost.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
        //设置超时的时间
        httppost.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, 1000*30);


        int status = httpClient.executeMethod(httppost);

        InputStream inputStream = httppost.getResponseBodyAsStream() ;
        String json = IOUtils.toString(inputStream,"UTF-8") ;
        httppost.releaseConnection();

        return json ;
    }


    public static String doPostForJson(String url, String param) throws Exception{
        return doPostForJson(url,param,null) ;
    }


    static{
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
            sslContext.init(null, new TrustManager[]{new MyX509TrustManager()}, new SecureRandom());

            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

