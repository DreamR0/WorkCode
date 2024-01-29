package com.api.customization.kq.qc2241191;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import weaver.general.BaseBean;

import java.util.HashMap;
import java.util.Map;

public class test {


    @Test
    public void testInterface(){
        String url="http://121.36.62.44:81/api/kq/kqReport/getKqReport";
        JSONObject params = new JSONObject();
        JSONObject data = new JSONObject();
        params.put("typeselect","7");
        params.put("status","8");
        params.put("viewScope","0");
        params.put("isNoAccount","0");
        data.put("data",params);

        System.out.println(data);

        String msmAccept = HttpRequest.get(url)
                .header(Header.CONTENT_TYPE, "application/json")//头信息，多个头信息多次调用此方法即可
                .body(JSONObject.toJSONString(data))//表单内容
                .timeout(20000)//超时，毫秒
                .execute().body();

        System.out.println("返回内容:" + msmAccept);
    }
}
