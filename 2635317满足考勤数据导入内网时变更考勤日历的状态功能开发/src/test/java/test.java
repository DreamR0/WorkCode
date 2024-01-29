import com.alibaba.fastjson.JSON;
import org.junit.Test;

import java.util.ArrayList;

public class test {

    @Test
    public void index(){
        String kqdate = "2023-08-16";
        String kqMonth = kqdate.substring(0,7);
        System.out.println(kqMonth);
    }
}
