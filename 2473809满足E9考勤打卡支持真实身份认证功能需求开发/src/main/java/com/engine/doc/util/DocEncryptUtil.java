package com.engine.doc.util;

import com.cloudstore.dev.api.util.TextUtil;
import com.engine.common.entity.TreeNode;
import weaver.conn.RecordSet;
import weaver.general.LabelUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author：tongh
 * date：2020-12-7 11:59
 * detail：
 */
public class DocEncryptUtil {
    //模块名称
    public static final String MOULDCODE = "DOCUMENT";
    //数据加密的表名
    public static final String CUS_FIELDDATA = "cus_fielddata";
    //自定义字段表 区分模块的字段
    public static final String SCOPE_DOCUMENTCUSTOM= "DocCustomFieldBySecCategory";
    //加密表名
    public static final String CUS_FIELDDATA_DOC_ENC = "cus_fielddata_doc_enc";
    //加密分享页面路由
    public static final String SHAREBASEROUTE = "SHAREBASE";
    //二次身份校验页面路由
    public static final String SECONDRAUTHOUTE = "NODE";

    public static final String ISENCRYPTSHARE = "isencryptshare";
    public static final String ENCRYPTRANGE = "encryptrange";
    public static final String ISENABLESECONDAUTH = "isEnableSecondAuth";
    public static final String ISENABLEDOUBLEAUTH = "isEnableDoubleAuth";
    public static final String AUTHVERIFIER = "authverifier";
    public static final String ISENCRYPT = "isencrypt";
    public static final String DESENSITIZATION = "desensitization";
    public static final String SECONDAUTH = "secondauth";
    /**
     * 获取加密字段设置 column组件信息
     * @param user
     * @return
     */
    public static List<Map<String,Object>> getFieldSetColumns(User user) {
        List<Map<String, Object>> columns = new ArrayList<Map<String,Object>>();
        String td1 = "fieldlabel,15456,1,1";//字段显示名
        String td2 = "fieldname,23241,1,1";//数据库字段名
        String td3 = "seccategoryname,385405,1,1"; //文档目录
        String td4 = "fieldtype,686,1,1";//字段类型
        String td5 = "desensitization,524048,4,1";//脱敏显示
        String td6 = "secondauth,524779,1,1";//二次身份校验
        String td7 = "isencrypt,17589,4,1";//加密

        String[] fields = new String[]{td1,td2,td3,td4,td5,td6,td7};
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
                col.put("width", "14%");
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
                col.put("width", "14%");
                col.put("com", comLists);
            }
            columns.add(col);
        }
        return columns;
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

    public static String getcategoryname(String key){
        String categoryname = "";
        RecordSet rs1 = new RecordSet();
        String sql1 = "select categoryname from docseccategory where id = ?";
        rs1.executeQuery(sql1,key);
        if(rs1.next()){
            categoryname = Util.null2String(rs1.getString("categoryname"));
        }
        return categoryname;
    }

    public static Map<String,String> EncryptInfo(int docid){
        Map<String,String> result = new HashMap<>();
        RecordSet rs = new RecordSet();
        String sql = "select isencryptshare,encryptrange,isEnableSecondAuth,isEnableDoubleAuth,authverifier from docseccategory where id = (select seccategory from  docdetail where id = ?)";

        result.put(ISENCRYPTSHARE,"");
        result.put(ENCRYPTRANGE,"");
        result.put(ISENABLESECONDAUTH,"");
        result.put(ISENABLEDOUBLEAUTH,"");
        result.put(AUTHVERIFIER,"");
        String sql1 = "select isenable from enc_secondauth_config_info where mouldcode=? and itemcode=?";
        rs.executeQuery(sql1,MOULDCODE,SECONDRAUTHOUTE);
        rs.next();
        String isenable = Util.null2String(rs.getString("isenable"));

        sql1 = "select isenable from enc_share_config_info where mouldcode=? and itemcode=? ";
        rs.executeQuery(sql1,MOULDCODE,SHAREBASEROUTE);
        rs.next();
        String share_isenable = Util.null2String(rs.getString("isenable"));

        rs.executeQuery(sql,docid);
        if(rs.next()){
            String isEnableSecondAuth = ("1".equals(Util.null2String(rs.getString("isEnableSecondAuth"))) && "1".equals(isenable)) ? "1":"0";
            String isencryptshare = ("1".equals(Util.null2String(rs.getString("isencryptshare"))) && "1".equals(share_isenable)) ? "1":"0";
            result.put(ISENCRYPTSHARE,isencryptshare);
            result.put(ENCRYPTRANGE,Util.null2String(rs.getString("encryptrange")));
            result.put(ISENABLESECONDAUTH,isEnableSecondAuth);
            result.put(ISENABLEDOUBLEAUTH,Util.null2String(rs.getString("isEnableDoubleAuth")));
            result.put(AUTHVERIFIER,Util.null2String(rs.getString("authverifier")));
        }
        return result;
    }

    public static String getFieldName(int fieldid){
        RecordSet rs = new RecordSet();
        String fieldName = "";
        String sql = "select fieldname from cus_formdict where id = ? and scope = ? ";
        rs.executeQuery(sql,fieldid,SCOPE_DOCUMENTCUSTOM);
        if(rs.next()){
            fieldName = Util.null2String(rs.getString("fieldname"));
        }
        return fieldName;
    }

    public static Map<String,String> getEncryptSet(int categoryid,String fieldname){
        RecordSet rs = new RecordSet();
        Map<String,String> result = new HashMap<>();
        String sql = "SELECT isencrypt,desensitization,secondauth from enc_field_config_info where mouldcode =?  and scope = ? and scopeid = ? and tablename = ? and fieldname = ?";
        rs.executeQuery(sql,DocEncryptUtil.MOULDCODE,DocEncryptUtil.SCOPE_DOCUMENTCUSTOM,categoryid,DocEncryptUtil.CUS_FIELDDATA,fieldname);
        rs.writeLog(">>>>> getEncryptSet categoryid="+categoryid+"  fieldname="+fieldname);
        result.put(ISENCRYPT,"");
        result.put(DESENSITIZATION,"");
        result.put(SECONDAUTH,"");
        if(rs.next()){
            result.put(ISENCRYPT,Util.null2String(rs.getString("isencrypt")));
            result.put(DESENSITIZATION,Util.null2String(rs.getString("desensitization")));
            result.put(SECONDAUTH,Util.null2String(rs.getString("secondauth")));
        }

        return result;
    }

    public static String getFieldtype(String id){
        RecordSet rs1 = new RecordSet();
        String sql1 = "select fieldhtmltype,fieldname from cus_formdict where id = ? and scope = ?";
        rs1.executeQuery(sql1,id,DocEncryptUtil.SCOPE_DOCUMENTCUSTOM);
        String fieldhtmltype = "";
        if(rs1.next()){
            fieldhtmltype = Util.null2String(rs1.getString("fieldhtmltype"));
        }
        return fieldhtmltype;
    }
    public static String getFieldtypebyName(String fieldname){
        RecordSet rs1 = new RecordSet();
        String sql1 = "select  fieldhtmltype  from  cus_formdict  where fieldname =? and scope=? order by id desc";
        rs1.executeQuery(sql1,fieldname,DocEncryptUtil.SCOPE_DOCUMENTCUSTOM);
        String fieldhtmltype = "";
        if(rs1.next()){
            fieldhtmltype = Util.null2String(rs1.getString("fieldhtmltype"));
        }
        return fieldhtmltype;
    }
}
