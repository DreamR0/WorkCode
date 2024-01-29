import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

public class test {
    @Test
    public void testInterface(){
        String url="http://10.10.42.237:8080/api/kq/kqReportCustom/getKqReport";
        JSONObject params = new JSONObject();
        JSONObject data = new JSONObject();
        params.put("typeselect","6");
        params.put("status","9");
        params.put("viewScope","0");
        params.put("isNoAccount","1");
        params.put("fromDate","2024-01-01");
        params.put("toDate","2024-01-31");
        params.put("attendanceSerial","");
        data.put("data",params);

        System.out.println(data);

        String msmAccept = HttpRequest.post(url)
                .header(Header.CONTENT_TYPE, "application/json")//头信息，多个头信息多次调用此方法即可
                .body(JSONObject.toJSONString(data))//表单内容
                .timeout(20000)//超时，毫秒
                .execute().body();

        System.out.println("返回内容:"+ msmAccept);
    }
}
