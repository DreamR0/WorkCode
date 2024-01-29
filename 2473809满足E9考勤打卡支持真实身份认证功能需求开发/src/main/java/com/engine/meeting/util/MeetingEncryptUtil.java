package com.engine.meeting.util;

import com.api.browser.bean.SearchConditionOption;
import com.api.meeting.util.FieldUtil;
import com.cloudstore.dev.api.util.TextUtil;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.entity.EncryptFieldEntity;
import com.engine.common.entity.EncryptShareSettingEntity;
import com.engine.common.entity.TreeNode;
import com.engine.common.enums.EncryptMould;
import com.engine.encrypt.biz.EncryptBasicConfigComInfo;
import com.engine.meeting.entity.MonitorSetBean;
import weaver.conn.RecordSet;
import weaver.general.LabelUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.meeting.MeetingUtil;
import weaver.mobile.webservices.common.ChatResourceShareManager;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeetingEncryptUtil {

    /**
     * 设置解密相关属性
     * @param recordSetArr 数据库操作对象
     */
    public static void setDecryptData2DaoInfo(Object... recordSetArr){
        setDecryptDaoInfo(true,recordSetArr);
    }

    /**
     * 设置解密相关属性
     * @param isReturnDecryptData 是否脱敏显示
     * @param recordSetArr 数据库操作对象
     */
    public static void setDecryptDaoInfo(boolean isReturnDecryptData, Object... recordSetArr){
        setDaoInfo(true, isReturnDecryptData, recordSetArr);
    }

    /**
     * 设置解密相关属性
     * @param isAutoDecrypt 是否自动解密
     * @param isReturnDecryptData 是否脱敏显示
     * @param recordSetArr 数据库操作对象
     */
    public static void setDaoInfo(boolean isAutoDecrypt, boolean isReturnDecryptData, Object... recordSetArr){
        if(null == recordSetArr) return;
        RecordSet rs;
        for(Object recordSet :recordSetArr){
            if(null == recordSet) continue;
            if(recordSet instanceof RecordSet){
                rs = (RecordSet) recordSet;
                rs.isAutoDecrypt(isAutoDecrypt);
                rs.isReturnDecryptData(isReturnDecryptData);
            }
        }
    }

    /**
     * 设置查看会议的数据库查询对象
     * 仅来自于分享权限：设置了数据库加密的字段强制密文显示
     * 仅来自于监控权限：设置了数据库加密的字段强制密文显示
     * 普通查看权限：根据后台设置的脱敏显示
     * @param meetingid 会议id
     * @param viewMeeting false：编辑；true：查看
     * @param user
     * @param params 前端请求参数
     * @param recordSetArr 数据库对象
     * @return false：无权限查看
     */
    public static boolean setMeetingDaoInfo(String meetingid, boolean viewMeeting, User user, Map<String, Object> params, Object... recordSetArr){
        boolean hasRight = false;
        Map rightMap = null;
        if (params.get("rightMap") != null && params.get("rightMap") instanceof Map) {//来自GetViewMeetingFieldCmd
            rightMap = (Map<String, Object>) params.get("rightMap");
            boolean canview = Boolean.parseBoolean(rightMap.get("canview")+"") ;
            MonitorSetBean monitorSetBean = (rightMap.get("monitorSetBean")!= null && rightMap.get("monitorSetBean") instanceof MonitorSetBean)
                    ? (MonitorSetBean) rightMap.get("monitorSetBean") : null;
            if(canview && monitorSetBean ==null) {//普通查看权限
                setDecryptDaoInfo(!viewMeeting, recordSetArr);
            }else {//分享或监控权限
                //其他情况监控权限、分享权限。设置了数据库加密的字段强制密文显示
                setDaoInfo(false,false,recordSetArr);
            }
            hasRight= true;

        }else{//单独访问api的情况
            MeetingUtil meetingUtil=new MeetingUtil();
            rightMap = meetingUtil.checkCanView(meetingid,user);
            boolean canview = Boolean.parseBoolean(rightMap.get("canview")+"") ;
            boolean canSecretRight = Boolean.parseBoolean(rightMap.get("canSecretRight")+"");
            MonitorSetBean monitorSetBean = (rightMap.get("monitorSetBean")!= null && rightMap.get("monitorSetBean") instanceof MonitorSetBean)
                    ? (MonitorSetBean) rightMap.get("monitorSetBean") : null;

            if(!canSecretRight) return false;

            boolean isfromchatshare = Util.null2String(params.get("isfromchatshare")).equals("1")?true:false;
            if(!canview){
                int share = Util.getIntValue(Util.null2String(params.get("sharer")));
                if(isfromchatshare){
                    ChatResourceShareManager crsm = new ChatResourceShareManager();
                    canview = crsm.authority(user,28, Util.getIntValue(meetingid,-1),share,-1);

                    if(canview){//判断分享加密解密的token
                        canview = MeetingEncryptUtil.checkShareToken(EncryptMould.MEETING, user, meetingid, String.valueOf(share), params);
                    }

                    if(canview){//分享权限 设置了数据库加密的字段强制密文显示
                        hasRight= true;
                        setDaoInfo(false,false,recordSetArr);
                    }
                }
            }else{
                hasRight= true;
                if(monitorSetBean ==null){//普通查看权限
                    setDecryptDaoInfo(!viewMeeting, recordSetArr);
                }else {//监控权限 设置了数据库加密的字段强制密文显示
                    setDaoInfo(false,false,recordSetArr);
                }

            }

        }
        return hasRight;

    }

    /**
     * 判断加密设置是否开启
     * @param mould
     * @return
     */
    public static boolean isOpenEncryptSet(EncryptMould mould){
        return EncryptConfigBiz.getEncryptEnable(mould.getCode());
    }

    /**
     * 判断分享加密设置是否开启
     * @param mould
     * @return
     */
    public static boolean isOpenShareEncryptSet(EncryptMould mould){
        return EncryptConfigBiz.getEncryptShareEnable(mould.getCode());
    }


    /**
     * 需要过滤的字段
     * key:字段名 value:scopeId
     */
    private static Map<String,String> needFilterFieldMap = new HashMap<String, String>();

    /**
     * 不需要加密的字段
     * key:字段名 value:scopeId
     */
    private static Map<String,String> cannotEncryptFieldMap = new HashMap<String, String>();

    static {
        //需要过滤的字段的系统字段
        needFilterFieldMap.put("name","1");
        needFilterFieldMap.put("customizeAddress","1");
        needFilterFieldMap.put("repeatdays","1");
        needFilterFieldMap.put("repeatweeks","1");
        needFilterFieldMap.put("repeatmonths","1");
        needFilterFieldMap.put("repeatmonthdays","1");
        needFilterFieldMap.put("remindHoursBeforeStart","1");
        needFilterFieldMap.put("remindTimesBeforeStart","1");
        needFilterFieldMap.put("remindHoursBeforeEnd","1");
        needFilterFieldMap.put("remindTimesBeforeEnd","1");
        needFilterFieldMap.put("othermembers","1");
        needFilterFieldMap.put("totalmember","1");
        needFilterFieldMap.put("crmtotalmember","1");

        //禁止加密的字段
        cannotEncryptFieldMap.put("subject","2");//议程主题
    }

    /**
     * 需要过滤的字段
     * @param scopeId //1:会议信息;2:会议议程;3:会议服务
     * @param fieldName 字段数据库名
     * @param fieldType 字段类型
     * @return
     */
    public static boolean needFilterField(int scopeId, String fieldName, String fieldType){
        //只支持单行文本和多行文本
        if(!"1".equals(fieldType) && !"2".equals(fieldType)) return true;
        return needFilterFieldMap.containsKey(fieldName) && String.valueOf(scopeId).equals(needFilterFieldMap.get(fieldName));
    }

    /**
     * 禁止加密的字段
     * @param scopeId
     * @param fieldName
     * @return
     */
    public static boolean cannotEncryptField(int scopeId, String fieldName){
        return cannotEncryptFieldMap.containsKey(fieldName) && String.valueOf(scopeId).equals(cannotEncryptFieldMap.get(fieldName));
    }

    /**
     * 封装详细设置字段 左侧树
     * @param user 查询用户
     * @param treeNodes 树
     * @param route 路由前缀
     * @param idArr 节点key
     * @param nameLabelArr 节点名称标签
     */
    public static void putTreeInfo(User user, List<TreeNode> treeNodes, String route, String[] idArr, String[] nameLabelArr ){
        TreeNode treeNode = null;
        for(int i=0;i<idArr.length;i++){
            treeNode = new TreeNode();
            treeNode.setDomid(idArr[i]);
            treeNode.setHaschild(false);
            treeNode.setKey(idArr[i]);
            treeNode.setName(SystemEnv.getHtmlLabelName(Util.getIntValue(nameLabelArr[i]),user.getLanguage()));
            treeNode.setRouteUrl(route+idArr[i]);
            if(Util.isEnableMultiLang()){
                treeNode.setMultiName(TextUtil.toBase64ForMultilang(new LabelUtil().getMultiLangLabel(nameLabelArr[i])));
            }
            treeNode.setAddChild(false);
            treeNode.setViewAttr(1);
            treeNode.setHasGroup(false);
            treeNode.setIsShow(i==0?"1":"0");

            treeNodes.add(treeNode);
        }
    }

    /**
     * 获取加密字段设置 column组件信息
     * @param user
     * @return
     */
    public static List<Map<String,Object>> getFieldSetColumns(User user) {
        List<Map<String, Object>> columns = new ArrayList<Map<String,Object>>();
        //字段显示名 数据库字段名 字段类型 脱敏显示 二次身份验证 加密
        String td1 = "fieldlabel,15456,1,1";//字段显示名
        String td2 = "fieldname,23241,1,1";//数据库字段名
        String td3 = "fieldtype,686,1,1";//字段类型
        String td4 = "desensitization,524048,4,1";//脱敏显示
        String td5 = "secondauth,524779,1,1";//二次身份校验
        String td6 = "isencrypt,17589,4,1";//加密

        String[] fields = new String[]{td1,td2,td3,td4,td5,td6};
        Map<String,Object> col = null;
        List<Object> comLists = new ArrayList<Object>();
        Map<String,Object> comMap = new HashMap<String, Object>();
        for(int i=0;i<fields.length;i++){
            col = new HashMap<String,Object>();
            comMap = new HashMap<String, Object>();
            comLists = new ArrayList<Object>();
            String[] fieldinfo = fields[i].split(",");
            String tmpkey = fieldinfo[0];

            col.put("title", SystemEnv.getHtmlLabelNames(fieldinfo[1], user.getLanguage()));
            col.put("key", tmpkey);
            col.put("dataIndex", tmpkey);
            col.put("colSpan", 1);
            if("desensitization".equals(fieldinfo[0])||"secondauth".equals(fieldinfo[0])||"isencrypt".equals(fieldinfo[0])){
                comMap.put("label", "");
                comMap.put("type", "CHECKBOX");
                comMap.put("key", tmpkey);
                comMap.put("viewAttr", "2");
                comLists.add(comMap);
                String[] checkVauleTypes = new String[]{"1","0"};
                col.put("showCheckAll", true);
                col.put("checkVauleType", checkVauleTypes);
                col.put("width", "16%");
                col.put("com", comLists);
//                if("secondauth".equals(fieldinfo[0])){
//                    col.put("tip",SystemEnv.getHtmlLabelName(524822, user.getLanguage()));
//                }
            }else{
                comMap.put("label", "");
                comMap.put("type", "INPUT");
                comMap.put("key", tmpkey);
                comMap.put("viewAttr", "1");
                comLists.add(comMap);
                col.put("width", "16%");
                col.put("com", comLists);
            }
            columns.add(col);
        }
        return columns;
    }

    /**
     * 封装字段设置信息
     * @param fieldInfoList 字段相关信息
     * @param user
     * @return
     */
    public static List<Map<String, Object>> getDatas(List fieldInfoList, User user){
        List<Map<String,Object>> datasList = new ArrayList<Map<String,Object>>();
        if(null == fieldInfoList || fieldInfoList.size()<=0) return datasList;
        EncryptBasicConfigComInfo encryptBasicConfigComInfo = new EncryptBasicConfigComInfo();
        encryptBasicConfigComInfo.next();

        EncryptFieldEntity encryptFieldEntity = null;
        Map<String,Object> data = null;
        //字段显示名 数据库字段名 字段类型 脱敏显示 二次身份验证 加密
        String td0 = "id";
        String td1 = "fieldlabel";
        String td2 = "fieldname";
        String td3 = "fieldtype";
        String td4 = "desensitization";
        String td5 = "secondauth";
        String td6 = "isencrypt";
        String td7 = "isencrypt_disable";
        String td8 = "tablename";

        String id;
        String fieldLabel;
        String fieldName;
        String fieldType;
        String fieldTypeName;
        String desensitization;
        String secondauth;
        String isencrypt;
        String isencrypt_disable;
        String tableName;
        for(int i=0; i<fieldInfoList.size();i++) {
            data = (Map<String,Object>)fieldInfoList.get(i);
            tableName = Util.null2String(data.get("tableName"));
            fieldName = Util.null2String(data.get("fieldName"));
            fieldLabel = Util.null2String(data.get("fieldLabel"));
            fieldType = Util.null2String(data.get("fieldType"));
            isencrypt_disable = Util.null2String(data.get("isencrypt_disable")).trim();
            fieldTypeName = SystemEnv.getHtmlLabelName(688, user.getLanguage());//单行文本框
            if("2".equals(fieldType)){
                isencrypt_disable="1";
                fieldTypeName = SystemEnv.getHtmlLabelName(689, user.getLanguage());//多行文本框
            }else if("".equals(isencrypt_disable)){
                isencrypt_disable="0";
            }

            encryptFieldEntity = EncryptConfigBiz.getFieldEncryptConfig(tableName,fieldName, true);
            id = "";
            isencrypt = "";
            desensitization = "";
            secondauth = "";
            if(encryptFieldEntity!=null) {
                id = encryptFieldEntity.getId();
                isencrypt = encryptFieldEntity.getIsEncrypt();
                desensitization = encryptFieldEntity.getDesensitization();
                secondauth = encryptFieldEntity.getSecondauth();
                if("1".equals(isencrypt)){
                    isencrypt_disable = "1";
                }
            }
            if(id.length()==0)id="random_"+ Util.getRandom();

            data = new HashMap<String, Object>();
            data.put(td0, id);
            data.put(td1, SystemEnv.getHtmlLabelNames(fieldLabel, user.getLanguage()));
            data.put(td2, fieldName);
            data.put(td3, fieldTypeName);
            data.put(td4, desensitization);
            data.put(td5, secondauth);
            data.put(td6, isencrypt);
            data.put(td7, "1".equals(encryptBasicConfigComInfo.getEnableStatus()) ? isencrypt_disable : "1");
            data.put(td8, tableName);
            datasList.add(data);
        }
        return datasList;
    }

    /**
     * 返回状态处理
     * @param targetMap
     * @param retCode
     * @param user
     */
    public static void putRetMapInfo(Map<String,Object> targetMap, String retCode, User user){
        if("-1".equals(retCode)){//未知错误
            targetMap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
        }else if("0".equals(retCode)){//无权限
            targetMap.put("message",  SystemEnv.getHtmlLabelName(2012,user.getLanguage()));
        }else if("-2".equals(retCode)){//参数错误
            targetMap.put("message",  SystemEnv.getHtmlLabelName(389204,user.getLanguage()));
        }
        targetMap.put("status", retCode);
    }

    /**
     * 获取加密范围选项
     * @param roomId
     * @param sqlWhere
     * @param languageId
     * @return
     */
    public static List getEncryptionRangeOption(String encryptionRange,User user) {

        boolean isAllSelect=!"2".equalsIgnoreCase(encryptionRange);
        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
        options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(527624, user.getLanguage()),isAllSelect));//全部加密分享
        options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(527625, user.getLanguage()),!isAllSelect));//允许个人设置加密分享
        return options;
    }


    /**
     * 获取分享按钮
     * @param mould 模块
     * @param shareBtnObj 原分享按钮对象
     * @param encryptBtnObj 加密后的按钮对象
     * @return
     */
    public static List getShareBtn(EncryptMould mould , Object shareBtnObj, Object encryptBtnObj){
        List btnList = new ArrayList();
        int shareType = 0;
        if(isOpenShareEncryptSet(mould)){
            EncryptShareSettingEntity shareSettingEntity = EncryptConfigBiz.getEncryptShareSetting(mould.getCode(),"SHAREBASE");
            if(null != shareSettingEntity && "1".equals(shareSettingEntity.getIsEnable())){
                shareType = Util.getIntValue(shareSettingEntity.getShareType(),0);
            }
        }

        //未开启加密分享 或 允许个人设置加密分享  使用原有分享
        if(shareType == 0 || shareType ==2 ){
            btnList.add(shareBtnObj);
        }

        //开启加密分享，增加加密分享按钮
        if(shareType>0){
            btnList.add(encryptBtnObj);
        }
        return btnList;
    }

    /**
     * 检查加密分享解密的有效性
     * @param mould 模块
     * @param user 访问对象
     * @param targetId 资源id
     * @param sharer 分享对象
     * @param params 请求参数
     * @return
     */
    public static boolean checkShareToken(EncryptMould mould , User user , String targetId, String sharer, Map<String,Object> params){
        HttpServletRequest request = null;
        if(null != params.get("httpServletRequest") &&  params.get("httpServletRequest") instanceof HttpServletRequest){
            request = (HttpServletRequest) params.get("httpServletRequest");
        }else{//非api访问，内部调用不用判断分享的token
            return true;
        }
        Map<String,Object> shareChkMap = new HashMap<String, Object>();
        shareChkMap.put("serialid", Util.null2String(params.get("serialid")));
        String resourcetype = "28";
        if(mould == EncryptMould.WORKPLAN){
            resourcetype = "2";
        }
        shareChkMap.put("resourcetype",resourcetype);
        shareChkMap.put("targetid",targetId);
        shareChkMap.put("sharer",sharer);
        return new EncryptConfigBiz().checkShareToken(mould.getCode(),"SHAREBASE",shareChkMap,request,user);
    }

    public static Map<String,String> EncryptInfo(String meetingid){
        Map<String,String> result = new HashMap<>();
        int isEnableSecondAuth = 0;
        String isEnableDoubleAuth = "";
        String authverifier = "";
        RecordSet rs = new RecordSet();
        String isenable = "";
        rs.executeQuery("select * from enc_secondauth_config_info where mouldcode =? and itemcode=? ","MEETING","NODE");
        if(rs.next()){
            isenable = rs.getString("isenable");
        }
        rs.executeQuery("select m1.isEnableSecondAuth as isEnableSecondAuth1,m2.isEnableSecondAuth as isEnableSecondAuth2,m2.isEnableDoubleAuth,m2.authverifier from meeting m1 left join Meeting_Type m2 on m1.meetingtype = m2.id where m1.id = ?",meetingid);
        if(rs.next()){
            int isEnableSecondAuth1 = rs.getInt("isEnableSecondAuth1");
            int isEnableSecondAuth2 = rs.getInt("isEnableSecondAuth2");
            if((isEnableSecondAuth1 == 1 || isEnableSecondAuth2 == 1) && isenable.equals("1")){
                isEnableSecondAuth = 1;
            }
            isEnableDoubleAuth = Util.null2String(rs.getString("isEnableDoubleAuth"));
            authverifier = Util.null2String(rs.getString("authverifier"));
        }
        result.put("isEnableSecondAuth",isEnableSecondAuth+"");
        result.put("isEnableDoubleAuth",isEnableDoubleAuth);
        result.put("authverifier",authverifier);
        return result;
    }

    public static Map<String,String> workplanEncryptInfo(String workplanid){
        Map<String,String> result = new HashMap<>();
        int isEnableSecondAuth = 0;
        String isEnableDoubleAuth = "";
        String authverifier = "";
        RecordSet rs = new RecordSet();
        String isenable = "";
        rs.executeQuery("select * from enc_secondauth_config_info where mouldcode =? and itemcode=? ","WORKPLAN","NODE");
        if(rs.next()){
            isenable = rs.getString("isenable");
        }
        rs.executeQuery("select m1.isEnableSecondAuth as isEnableSecondAuth1,m2.isEnableSecondAuth as isEnableSecondAuth2,m2.isEnableDoubleAuth,m2.authverifier from workplan m1 left join workplantype m2 on m1.type_n = m2.workplantypeid where m1.id = ?",workplanid);
        if(rs.next()){
            int isEnableSecondAuth1 = rs.getInt("isEnableSecondAuth1");
            int isEnableSecondAuth2 = rs.getInt("isEnableSecondAuth2");
            if((isEnableSecondAuth1 == 1 || isEnableSecondAuth2 == 1) && isenable.equals("1")){
                isEnableSecondAuth = 1;
            }
            isEnableDoubleAuth = Util.null2String(rs.getString("isEnableDoubleAuth"));
            authverifier = Util.null2String(rs.getString("authverifier"));
        }
        result.put("isEnableSecondAuth",isEnableSecondAuth+"");
        result.put("isEnableDoubleAuth",isEnableDoubleAuth);
        result.put("authverifier",authverifier);
        return result;
    }

}
