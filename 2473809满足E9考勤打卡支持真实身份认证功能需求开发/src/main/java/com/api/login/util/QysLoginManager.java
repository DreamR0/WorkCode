package com.api.login.util;

import com.alibaba.fastjson.JSONObject;
import com.api.common.cmd.login.DoUserSessionCmd;
import com.api.login.bean.QysLoginManagerBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.loginstrategy.LoginStrategyManager;
import weaver.hrm.schedule.ext.util.HttpUtil;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.login.exception.CaCheckException;
import weaver.systeminfo.SystemEnv;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author gecy
 * @date 2021/05/15 10:16
 **/
public class QysLoginManager {

    private static final String QYS_LOGIN_STRATEGY = "7" ;

    public static void writeLog(Object obj) {
        writeLog(QysLoginManager.class.getName(),obj);
    }

    public static void writeLog(String classname , Object obj)  {
        Log log= LogFactory.getLog(classname);
        if(obj instanceof Exception)
            log.error(classname ,(Exception)obj);
        else{
            log.error(obj);
        }
    }


    public static boolean isOpenQysLogin(String userid,HttpServletRequest request){
        boolean ismobile = request == null ? false : isRealMobile(request) ;
        String ip = request == null ? null : Util.getIpAddr(request) ;
        return isOpenQysLogin(userid,ismobile,ip) ;
    }

    public static  boolean isOpenQysLogin(String userid,boolean ismobile){
        return isOpenQysLogin(userid,ismobile,null) ;
    }

    public static  boolean isOpenQysLogin(String userid,boolean ismobile,String ip){


        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();
        writeLog("isOpenQysLogin["+userid+"]>"+settings.getNeedCL());
        if(!"1".equalsIgnoreCase(Util.null2String(settings.getNeedCL()))) return false ; // 大开关未开

        RecordSet rs = new RecordSet() ;
        rs.executeQuery("select userusbtype,usbstate,usbscope from hrmresourcemanager where id=?",userid) ;
        boolean isadmin = false ;
        if(!rs.next()){ // 非管理员
            rs.executeQuery("select userusbtype,usbstate,usbscope from hrmresource where id=?",userid) ;
            rs.next() ;
            isadmin = false ;
        }else{
            isadmin = true ;
        }
        String userusbtype = rs.getString(1) ;
        String usbstate = rs.getString(2) ;
        String usbscope = rs.getString("usbscope") ;
        writeLog("userusbtype["+userid+"]>"+userusbtype+";;usbstate="+usbstate+";;usbscope="+usbscope+";;ismobile="+ismobile+";;");

        if(!QYS_LOGIN_STRATEGY.equalsIgnoreCase(userusbtype)) return false ;

        // 网段策略
        if("2".equalsIgnoreCase(usbstate) && !ismobile && !LoginStrategyManager.isInIp(ip)) return true ;

        if("0".equalsIgnoreCase(usbstate) && checkUsbScopeOn(usbscope,ismobile)) return true ;

        return false ;
    }

    public static boolean checkUsbScopeOn(String scope,boolean ismobile){
        if(StringUtils.isBlank(scope)) scope = "1" ;
        if("1".equalsIgnoreCase(scope)) return true ;
        if("2".equalsIgnoreCase(scope) && !ismobile) return true ;
        if("3".equalsIgnoreCase(scope) && ismobile) return true ;

        return  false ;
    }

//    public static boolean checkOnByUsbStateWithOutNetWork(boolean ismobile,String usbstate){
//        if("0".equalsIgnoreCase(usbstate)) return true ;
//        if("1".equalsIgnoreCase(usbstate)) return false ;
//        if("3".equalsIgnoreCase(usbstate) && !ismobile) return true ;
//        if("4".equalsIgnoreCase(usbstate) && ismobile) return true ;
//        return false ;
//    }

    private static String getAppKey(RemindSettings settings){
        return Util.null2String(settings.getAppKey()) ;
    }

    private static String getAppSecret(RemindSettings settings){
        return Util.null2String(settings.getAppSecret()) ;
    }

    private static String getUrl(RemindSettings settings){
        return Util.null2String(settings.getAddressCL());
    }

    public static void removeSessionUser(HttpServletRequest request){
        request.getSession(true).removeAttribute("weaver_user@bean" );
        writeLog("removeSessionUser success!");
    }


    private static String buildMd5Token(String userid){
        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();
        String appSecret = getAppSecret(settings);

        long t = System.currentTimeMillis() ;
        long min = 1000*60 ;
        t = t / min * min ;

        String orgin = buildTokenOrgin(appSecret,userid,t) ;
        return Util.getEncrypt(orgin) ;
    }

    private static String buildTokenOrgin(String appSecret,String userid,long time){
        return appSecret.concat("_").concat(userid).concat("_")+(time) ;
    }


    public static boolean isRealMobile(HttpServletRequest request){
        String header = Util.null2String(request.getHeader("user-agent")) ;
        String ismobile = Util.null2String(request.getParameter("ismobile"));
        boolean isParamsMobile = "1".equalsIgnoreCase(ismobile) ;
        boolean isPcMobile = header.contains("Electron") || header.contains("nw/0.14.7") ;
        return isParamsMobile && !isPcMobile ;
    }


    /**
     * 检查token是否有效
     * @param token
     * @param userid
     * @return
     */
    public static boolean checkToken(String token,String userid){

        long min = 1000*60 ;
        long t = System.currentTimeMillis() / min * min;
        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();
        String appSecret = getAppSecret(settings);

        boolean result = Arrays.asList(-2, -1, 0, 1, 2, 3, 4, 5).stream().map(i -> {
            long seed = t + min * i;
            return Util.getEncrypt(buildTokenOrgin(appSecret, userid, seed));
        }).anyMatch(stoken -> stoken.equalsIgnoreCase(token));

        writeLog("checkToken>"+token+";userid="+userid+";;"+result);

        return result;
    }


    public static Map<String,Object> statusAuth(HttpServletRequest request)throws CaCheckException{
        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();

        Map<String, Object> returnMap = new HashMap<>() ;
        String authId = request.getParameter("authId") ;
        String langid = request.getParameter("langid") ;
        Map<String, String> map = new HashMap<>();
        map.put("authId", authId);
        map.put("type", "Authorization");

        Map<String, String> headerMap = getHeaderMap();

        String json = null ;
        String req_params = JSONObject.toJSONString(map) ;
        String url = getUrl(settings) + "/app/person/auth/person/auth/status" ;

        try {
            json = HttpUtil.doPostForJson(url, req_params, headerMap);
            writeLog("invoke[statusAuth] json>" + json + " ; for>>" + JSONObject.toJSONString(map) + "; header>" + JSONObject.toJSONString(headerMap));

            Map<String, Object> data = JSONObject.parseObject(json);
            if (0 == Util.getIntValue(data.get("code") + "", -1)) {
                Map<String, Object> result = (Map<String, Object>) data.get("result");
                int status = Util.getIntValue(result.get("status")+"",-1) ;
                if(status == -1 || status == 0){
                    returnMap.put("code", "0");
                    return returnMap;
                }else if(status == 1){
                    String bizId = Util.null2String(result.get("bizId"));
                    String userid = decoderUserId(bizId);
                    String token = buildMd5Token(userid) ;
                    writeLog("buildMd5Token>>"+token+";;"+userid);
                    returnMap.put("token",token) ;
                    returnMap.put("userid",userid) ;
                    returnMap.put("code", "1");
                }else{
                    throw new CaCheckException("-1", SystemEnv.getHtmlLabelName(534480 ,Util.getIntValue(langid,7))+":" + status);
                }
            } else {
                // 失败
                String msg = Util.null2String(data.get("message")) ;
                throw new CaCheckException("-1", SystemEnv.getHtmlLabelName(534480 ,Util.getIntValue(langid,7))+":"+msg);
            }
        }catch (CaCheckException e){
            throw e ;
        }catch (Exception e){
            writeLog(e);
            throw new CaCheckException("-1",SystemEnv.getHtmlLabelName(534480 ,Util.getIntValue(langid,7))) ;
        }

        RecordSet rs = new RecordSet() ;
        rs.executeUpdate("insert into hrm_qyslogin_log(authId,url,req_params,req_header,resp_params,ts) values (?,?,?,?,?,?)",
                authId,url,req_params,JSONObject.toJSONString(headerMap),json,DateUtil.getFullDate()) ;

        return returnMap ;
    }

    public static Map<String,Object> statusAuthAndBuildUser(HttpServletRequest request,HttpServletResponse response)throws CaCheckException{
        Map<String, Object> map = statusAuth(request);
        if("1".equalsIgnoreCase(map.get("code")+"")){
            String langid = request.getParameter("langid") ;
            buildUser(request, response, map.get("userid")+"", langid);
        }
        return map ;
    }


    private static void buildUser(HttpServletRequest request,HttpServletResponse response,String userid,String langid){
        Map<String, Object> params = new HashMap<>();
        String id = Util.null2String(userid);
        params.put("userid", id);
        params.put("languid", ""+langid);
        DoUserSessionCmd cmd = new DoUserSessionCmd(request, response, params);
        cmd.execute(null);
    }

    public static String[] initAuth(String userid) throws CaCheckException{
        QysLoginManagerBean qysLoginManagerBean = new QysLoginManagerBean();
        qysLoginManagerBean.setUserid(userid);
        qysLoginManagerBean.setUsername(null);
        qysLoginManagerBean.setLangid("7");
        qysLoginManagerBean.setSecondAuth(false);
        return initAuth(qysLoginManagerBean) ;
    }

    public static String[] initAuth(String userid,String langid) throws CaCheckException{
        return null;
    }
	
	public static String[] initAuth(QysLoginManagerBean qysLoginManagerBean) throws CaCheckException{
        return initAuth(qysLoginManagerBean,null);
    }

    public static String[] initAuth(QysLoginManagerBean qysLoginManagerBean,String requestHost) throws CaCheckException{
        
        String userid = qysLoginManagerBean.getUserid();
        String username = qysLoginManagerBean.getUsername();
        String langid = qysLoginManagerBean.getLangid();
        boolean isSecondAuth = qysLoginManagerBean.isSecondAuth();

        String language = "8".equalsIgnoreCase(langid) ? "en_US":"zh_CN" ;
        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();
        String globalClAuthtype = settings.getClAuthtype();
        String hideSuccessStatusPage = Util.null2String(settings.getHideSuccessStatusPage());
        RecordSet rs = new RecordSet() ;
        rs.executeQuery("SELECT CERTIFICATENUM ,LASTNAME, clAuthtype, mobile, accountid1 FROM HRMRESOURCE  WHERE id=?",userid) ;
        String cardNo = "" ;
        String mobile = "" ;
        String bankNo = "" ;
        String name = username ;
        String clAuthtype = "FACE" ;
        if(rs.next()){
            cardNo = Util.null2String(rs.getString("CERTIFICATENUM"));
            name = Util.null2String(rs.getString("LASTNAME"));
            bankNo = Util.null2String(rs.getString("accountid1"));
            mobile = Util.null2String(rs.getString("mobile"));
            clAuthtype = Util.null2String(rs.getString("clAuthtype"));
            if(isSecondAuth){
                //二次认证走后台开关
                clAuthtype = settings.getClAuthTypeDefault();
            }
            if(globalClAuthtype.toUpperCase().indexOf(clAuthtype.toUpperCase())==-1){
                //全局未开启，走默认
                clAuthtype = "";
            }
            if(clAuthtype.equals("")){
                clAuthtype = "FACE";
            }
            if(StringUtils.isNotBlank(name)){
                String[] split = name.split("[(（]");
                if(split.length > 0) name = split[0] ;
            }
        }
        name = name.trim() ;


        String bizId = encoderUserId(userid);
        List modifyFields = new ArrayList();
        modifyFields.add("NAME");
        modifyFields.add("CARDNO");
        modifyFields.add("MOBILE");
        modifyFields.add("BANKNO");
        modifyFields.add("BANKPHONE");
        if(Util.null2String(username).length()>0){
            //合同外发编辑姓名不让修改
            modifyFields.remove("NAME");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("type", "Authorization");
        params.put("mode", clAuthtype);
        params.put("channel", "THIRDAPP");
        params.put("livinessAccessed","true") ;
        params.put("name",name) ;
        params.put("cardNo",cardNo) ;
        params.put("mobile",mobile) ;
        params.put("bankNo",bankNo) ;
        params.put("bankPhone",mobile) ;
        params.put("bizId", bizId);
        params.put("language",language) ;
        params.put("modifyFields",modifyFields) ;
        params.put("hideSuccessStatusPage",!hideSuccessStatusPage.equals("1")) ;
		
		if(StringUtils.isNotBlank(requestHost)){

            params.put("callbackUrl",requestHost) ;
        }


        Map<String, String> headerMap = getHeaderMap();
        try {
            String json = HttpUtil.doPostForJson(getUrl(settings) + "/app/person/auth/person/auth/init", JSONObject.toJSONString(params),headerMap );
            writeLog("invoke[initAuth] json>" + json + " ; for>>" + JSONObject.toJSONString(params) + "; header>" + JSONObject.toJSONString(headerMap));
            Map<String, Object> data = JSONObject.parseObject(json);
            if (0 == Util.getIntValue(data.get("code") + "", -1)) {
                Map<String, Object> result = (Map<String, Object>) data.get("result");
                String authId = Util.null2String(result.get("authId"));
                String authUrl = Util.null2String(result.get("authUrl"));
                String authType = Util.null2String(result.get("authType")) ;
                String errorFlag = Util.null2String(result.get("errorFlag")) ;
                if(StringUtils.isBlank(authType)){
                    authType = "FACE" ;
                }
                if("true".equals(errorFlag)) {
                    rs.executeUpdate("insert into hrm_qyslogin_log(authId,url,req_params,req_header,resp_params,ts) values (?,?,?,?,?,?)",
                            authId, authUrl, JSONObject.toJSONString(params), JSONObject.toJSONString(headerMap), json, DateUtil.getFullDate());
                }
                return new String[]{authId, authUrl,authType,errorFlag};
            } else {
                // 失败
                String msg = Util.null2String(data.get("message")) ;
                throw new CaCheckException("-1", SystemEnv.getHtmlLabelName(534918 ,Util.getDefaultLang()));
            }
        }catch (CaCheckException e){
            throw e ;
        }catch (Exception e){
            writeLog(e);
            throw new CaCheckException("-1",SystemEnv.getHtmlLabelName(534480 ,Util.getDefaultLang())) ;
        }

    }

    /***
     * 查询身份认证绑定关系
     * @param user
     * @return
     * @throws CaCheckException
     */
    public static boolean queryBind(User user)throws CaCheckException{
        boolean bindStatus = false;
        Map<String, String> map = new HashMap<>();
        int langid = user.getLanguage();
        map.put("bizId", ""+user.getUID());
        Map<String, String> headerMap = getHeaderMap();

        String json = null ;
        String req_params = JSONObject.toJSONString(map) ;
        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();
        String url = getUrl(settings) + "/app/person/auth/person/auth/query-bind" ;

        try {
            json = HttpUtil.doPostForJson(url, req_params, headerMap);
            writeLog("invoke[queryBind] json>" + json + " ; for>>" + JSONObject.toJSONString(map) + "; header>" + JSONObject.toJSONString(headerMap));

            Map<String, Object> data = JSONObject.parseObject(json);
            if (0 == Util.getIntValue(data.get("code") + "", -1)) {
                bindStatus = (Boolean) data.get("result");
            } else {
                // 失败
                String msg = Util.null2String(data.get("message")) ;
                throw new CaCheckException("-1", SystemEnv.getHtmlLabelName(534480 ,langid)+":"+msg);
            }
        }catch (CaCheckException e){
            throw e ;
        }catch (Exception e){
            writeLog(e);
            throw new CaCheckException("-1",SystemEnv.getHtmlLabelName(534480 ,langid)) ;
        }
        return bindStatus;
    }

    /***
     * 解除身份认证绑定关系
     * @param user
     * @return
     * @throws CaCheckException
     */
    public static boolean unBind(User user)throws CaCheckException{
        boolean unbindStatus = false;
        Map<String, String> map = new HashMap<>();
        int langid = user.getLanguage();
        map.put("bizId", ""+user.getUID());
        Map<String, String> headerMap = getHeaderMap();

        String json = null ;
        String req_params = JSONObject.toJSONString(map) ;
        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();
        String url = getUrl(settings) + "/app/person/auth/person/auth/unbind" ;

        try {
            json = HttpUtil.doPostForJson(url, req_params, headerMap);
            writeLog("invoke[unbind] json>" + json + " ; for>>" + JSONObject.toJSONString(map) + "; header>" + JSONObject.toJSONString(headerMap));

            Map<String, Object> data = JSONObject.parseObject(json);
            if (0 == Util.getIntValue(data.get("code") + "", -1)) {
                unbindStatus = (Boolean) data.get("result");
            } else {
                // 失败
                String msg = Util.null2String(data.get("message")) ;
                throw new CaCheckException("-1", SystemEnv.getHtmlLabelName(534480 ,langid)+":"+msg);
            }
        }catch (CaCheckException e){
            throw e ;
        }catch (Exception e){
            writeLog(e);
            throw new CaCheckException("-1",SystemEnv.getHtmlLabelName(534480 ,langid)) ;
        }
        return unbindStatus;
    }

    private static String encoderUserId(String userid){
        return userid ;
    }

    private static String decoderUserId(String encoder){
        return encoder ;
    }


    private static Map<String, String> getHeaderMap()  {
        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();

        String appKey = getAppKey(settings); // 由契约锁提供
        String appSecret = getAppSecret(settings); // 由契约锁提供
        String nonce = UUID.randomUUID().toString();
        String signature = sign(appSecret, nonce);
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("x-qys-app-key", appKey);
        headerMap.put("x-qys-app-nonce", nonce);
        headerMap.put("x-qys-app-sign", signature);
        return headerMap;
    }


    private static String sign(String appSecret, String nonce) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKey);
            byte[] bytes = mac.doFinal(nonce.getBytes("UTF-8"));
            return bytesToHexString(bytes);
        }catch (Exception e){
            writeLog(e);
            throw new RuntimeException(e) ;
        }
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        for (byte b : bytes) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

}
