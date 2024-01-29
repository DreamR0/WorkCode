package com.api.hrm.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.crm.Maint.CustomerInfoComInfo;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.definedfield.HrmFieldManager;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;

public class HrmTransMethod extends BaseBean {

    public String getDepartmentNameByResourceId(String arg, String key) {
        String departmentname = "";
        if (key.length() > 0) {
            try {
                ResourceComInfo ResourceComInfo = new ResourceComInfo();
                DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
                String departmentid = ResourceComInfo.getDepartmentID(key);
                departmentname = DepartmentComInfo.getDepartmentname(departmentid);
            } catch (Exception e) {
                writeLog(e);
            }
        }
        return departmentname;
    }

    public String getSubCompanyNameByResourceId(String arg, String key) {
        String subcompanyname = "";
        if (key.length() > 0) {
            try {
                ResourceComInfo ResourceComInfo = new ResourceComInfo();
                SubCompanyComInfo SubCompanyComInfo = new SubCompanyComInfo();
                String subcompanyid = ResourceComInfo.getSubCompanyID(key);
                subcompanyname = SubCompanyComInfo.getSubcompanyname(subcompanyid);
            } catch (Exception e) {
                writeLog(e);
            }
        }
        return subcompanyname;
    }

    public ArrayList<String> getHrmGroupBaseOperate(String id, String canEdit, String type) {
        ArrayList<String> resultList = new ArrayList<String>();
        resultList.add("true");//编辑
        if (type.equals("0") || canEdit.equals("true")) {//成员
            resultList.add("true");
        } else {
            resultList.add("false");
        }

        if (type.equals("1") && canEdit.equals("true")) {//共享范围
            resultList.add("true");
        } else {
            resultList.add("false");
        }

        resultList.add(getHrmGroupCanCancel(id));

        resultList.add(getHrmGroupCanIsCancel(id));

        resultList.add("true");//删除

        return resultList;
    }

    public String getHrmGroupCanCancel(String id){
        RecordSet rs = new RecordSet();
        boolean canCancel = true;
        String sql = "select canceled from HrmGroup where id = ?";
        rs.executeQuery(sql,id);
        if(rs.next()){
            String canceled = Util.null2String(rs.getString("canceled"));
            if(canceled.equals("1")){
                canCancel = false;
            }
        }

        if(canCancel){
            return "true";
        }else{
            return "false";
        }
    }

    public String getHrmGroupCanIsCancel(String id){
        RecordSet rs = new RecordSet();
        String canceled = "";
        String sql = "select canceled from HrmGroup where id = ?";
        rs.executeQuery(sql,id);
        if(rs.next()){
            canceled = Util.null2String(rs.getString("canceled"));
        }
        if(canceled.equals("1")){
            return "true";
        }else{
            return "false";
        }
    }

    public ArrayList<String> getHrmSearchOperate(String id, String type) {
        ArrayList<String> resultList = new ArrayList<String>();

        String suggesttype = type.trim();

        if (suggesttype.equals("1")) {
            resultList.add("true");
            resultList.add("false");
            resultList.add("true");
        } else {

            resultList.add("false");
            resultList.add("true");
            resultList.add("true");
        }

        return resultList;
    }


    public String getSignTypeShowName(String signtype, String strlanguage) {
        int language = Util.getIntValue(strlanguage, 7);
        String showName = "";
        if ("hrm_sign".equalsIgnoreCase(signtype.toLowerCase())) {
            showName = SystemEnv.getHtmlLabelName(132104, language);
        } else if ("mobile_sign".equalsIgnoreCase(signtype.toLowerCase())) {
            showName = SystemEnv.getHtmlLabelName(132105, language);
        } else if ("e9_mobile_out".equalsIgnoreCase(signtype.toLowerCase())) {
            showName = SystemEnv.getHtmlLabelName(132105, language);
        } else {
            showName = signtype;
        }
        return showName;
    }

    public String getCrm(String crm){
        try {
            CustomerInfoComInfo customerInfoComInfo = new CustomerInfoComInfo();
            List<String> crmList = new ArrayList<>();
            crm = Util.null2String(crm);
            if (!"".equals(crm)) {
                List<String> crmIds = Util.splitString2List(crm, ",");
                for (String key : crmIds) {
                    crmList.add(customerInfoComInfo.getCustomerInfoname(key));
                }
            }
            return String.join(",", crmList);
        }catch (Exception ex){
            return "";
        }
    }

    public String getSignRemark(String remark, String params) {
        String[] arrParams = params.split("\\+");
        String signtype = arrParams[0];
        int language = Util.getIntValue(arrParams[1], 7);

//        if ("2".equals(signtype)) {
//            remark = SystemEnv.getHtmlLabelName(125526, language);
//        }
        if ("".equals(remark)) {
            remark = SystemEnv.getHtmlLabelName(125527, language);
        }
        return remark;
    }

    public String getHrmGroupName1(String name, String otherPara) {
        String canceled = otherPara.split("\\+")[0];
        String strlanguage = otherPara.split("\\+")[1];
        int language = Util.getIntValue(strlanguage);
        if (canceled.equals("1")) {
            name = name + "(<span style=\"color:#F00\">"+ SystemEnv.getHtmlLabelName(22151, language)+"</span>)";
        }
        return name;
    }

    /*
     * 转换常用组类型为中文名称
     */
    public String getHrmGroupTypeName(String type, String strlanguage) {

        int language = Util.getIntValue(strlanguage);

        if (type.equals("1")) {
            return "" + SystemEnv.getHtmlLabelName(17619, language);
        } else {
            return "" + SystemEnv.getHtmlLabelName(17618, language);
        }

    }


    /*
     * 共享范围 是否包含下级
     */
    public String isAlllevel(String type, String str) {

        String[] arrParams = str.split("\\+");
        int language = Integer.parseInt(arrParams[1]);
        String sharetype = arrParams[0];
        if (sharetype.equals("2") || sharetype.equals("3")) {
            if (type.equals("1")) {
                return "" + SystemEnv.getHtmlLabelName(163, language);
            } else {
                return "" + SystemEnv.getHtmlLabelName(161, language);
            }
        } else {
            return " ";
        }

    }


    /*
     * LINK常用组成员数量
     */
    public String getHrmGroupMemberNum(String result) {

        int num = Util.getIntValue(result);
        return "<a  href=\"javascript:void(0);\"  onClick=\"doMember();\" >" + num + "</a>";
    }

    public String getHrmGroupName(String id, String name) {
        return name;
    }

    /*
     * 将常用组序号  从-1 转为  1
     */
    public String getHrmDspOrder(String dsporder) {

        if (dsporder.trim().equals("-1") || dsporder.trim().equals("") || dsporder == null) {
            String dsp = "1";
            return dsp;

        } else {

            return dsporder;
        }

    }


    public String getEmessageCheckbox(String params) {
        return "true";
    }


    public String getAllParentDepartmentNames(String id, String deptid, String subcomid) throws Exception {
        String names = "";
        try {

            DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
            names = DepartmentComInfo.getAllParentDepartmentNames(deptid, subcomid);

        } catch (Exception e) {
            new BaseBean().writeLog(e);
        }

        return names;
    }


    public String getHrmDepartmentLink(String name, String id) {

        return "<a href='/hrm/HrmTab.jsp?_fromURL=HrmDepartmentDsp&id=" + id + "' target='_blank' rel='external nofollow'  >" + name + "</a>";

    }


    public String getHrmLink(String name, String id) {

        return "<a href='/hrm/HrmTab.jsp?_fromURL=HrmResource&id=" + id + "' target='_blank' rel='external nofollow'  >" + name + "</a>";

    }


    public String getHrmSubcompanyLink(String id) {
        String name = "";
        try {
            SubCompanyComInfo sComInfo = new SubCompanyComInfo();
            name = sComInfo.getSubcompanyname(id);
        } catch (Exception e) {
            // TODO: handle exception

            new BaseBean().writeLog(e);
        }
        return "<a href='/hrm/HrmTab.jsp?_fromURL=HrmSubCompanyDsp&id=" + id + "' target='_blank' rel='external nofollow'  >" + name + "</a>";
    }


    public String getHrmSubcompanyLink(String name, String id) {

        return "<a href='/hrm/HrmTab.jsp?_fromURL=HrmSubCompanyDsp&id=" + id + "' target='_blank' rel='external nofollow'  >" + name + "</a>";
    }


    public String getJobTitleLink(String name, String id) {

        return "<a href='hrm/HrmDialogTab.jsp?_fromURL=HrmJobTitlesEdit&id=" + id + "' target='_blank' rel='external nofollow'  >" + name + "</a>";
    }


    public String getHrmDepartmentLink(String id) {
        String name = "";
        try {
            DepartmentComInfo dComInfo = new DepartmentComInfo();
            name = dComInfo.getDepartmentname(id);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "<a href='/hrm/HrmTab.jsp?_fromURL=HrmDepartmentDsp&id=" + id + "' target='_blank' rel='external nofollow'  >" + name + "</a>";

    }


    public String getAddRpPercent(String molecular, String total, String languageid) {
        return StringUtil.formatDoubleValue(String.valueOf(molecular), String.valueOf(total));
    }


    public String getTypeName(String typeInt, String languageidint) {

        int languageid = Integer.parseInt(languageidint);
        int type = Integer.parseInt(typeInt);
        String name = (type == 1) ? SystemEnv.getHtmlLabelName(6094, languageid) : ((type == 5) ? (SystemEnv.getHtmlLabelName(6091, languageid)) : (SystemEnv.getHtmlLabelName(6092, languageid)));
        return name;
    }

    /**
     * 判断CheckBox是否可以勾选(如果招聘信息中引用了此招聘计划，此招聘计划不可删除)
     *
     * @param careerplanid
     * @return
     */
    public boolean getCareerPlanCheckbox(String careerplanid) {
        String sql = "select * from HrmCareerInvite where careerplanid=" + careerplanid;
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery(sql);
        if (recordSet.next()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 判断是否存在签到图片
     *
     * @param param
     * @return
     */
    public String canViewSignImg(String param) {
        String showImg = "0";
        new BaseBean().writeLog("==zj==(params)" + JSON.toJSONString(param));
        if (param.indexOf("sign") > -1) {
            param = param.substring(4);
            String sql = "select * from Mobile_Sign where id=? ";
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(sql, param);
            if (recordSet.next()) {
                String attachment = recordSet.getString("attachment");
                if (attachment.length() > 0) {
                    showImg = "1";
                }
            }
        }
        if (param.indexOf("hrm") > -1){
                //qc2535208查找签到表是否存在签到图片
                RecordSet rs = new RecordSet();
                param = param.substring(3);
               String sql = "select * from HrmScheduleSign where id=?";
                new BaseBean().writeLog("==zj==(param)" + JSON.toJSONString(param));
                rs.executeQuery(sql,param);
                if (rs.next()) {
                    String attachment = rs.getString("attachment");
                    if (attachment.length() > 0) {
                        showImg = "1";
                    }
                }
        }
        return showImg;
    }

    public String getOperateType(String operateType, String language) {
        if (operateType.equals("sync")) {
            return SystemEnv.getHtmlLabelName(18240, Util.getIntValue(language, 7));//同步
        } else if (operateType.equals("excel")) {
            return SystemEnv.getHtmlLabelName(28343, Util.getIntValue(language, 7));//导出Excel
        } else {
            return operateType;
        }
    }

    public String getOperateItem(String operateItem, String language) {
        if (operateItem.equals("507")) {
            return SystemEnv.getHtmlLabelName(1515, Util.getIntValue(language, 7));//通讯录
        } else {
            return operateItem;
        }
    }

    public String getRelatedName(String relatedName, String language) {
        if (relatedName.equals("showCol")) {
            return SystemEnv.getHtmlLabelName(33541, Util.getIntValue(language, 7));//显示列
        } else if (relatedName.equals("excel")) {
            return SystemEnv.getHtmlLabelName(1515, Util.getIntValue(language, 7));//通讯录
        } else {
            return relatedName;
        }
    }

    public String getOperateDesc(String operateDesc, String otherParams) {
        String resultStr = "";
        try {
            String[] otherParamArr = otherParams.split("\\+");
            String operateType = otherParamArr[0];
            String language = otherParamArr[1];

            if (operateType.equals("excel")) {
                String other_operatedesc = Util.null2s(otherParamArr[2],"");
                if(other_operatedesc.trim().length() > 0){
                  operateDesc = other_operatedesc;
                }
                return operateDesc;
            }

            Map<String, String> fieldMap = new HashMap<String, String>();
            HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", -1);
            hfm.getCustomFields();
            while (hfm.next()) {
                String fieldName = hfm.getFieldname();
                String fieldLabel = hfm.getLable();
                if (fieldName.equalsIgnoreCase("loginid") || fieldName.equalsIgnoreCase("systemlanguage")) {
                    continue;
                }
                if (fieldName.indexOf("field") > -1) {
                    fieldName = "t0_" + fieldName;
                }
                fieldMap.put(fieldName, fieldLabel);
            }
            fieldMap.put("subcompanyid1","141");
            String[] operateDescArr = operateDesc.split(",");
            for (int i = 0; i < operateDescArr.length; i++) {
                String _fieldName = operateDescArr[i];
                String _fieldLabel = fieldMap.get(_fieldName);
                resultStr += "、" + SystemEnv.getHtmlLabelNames(_fieldLabel, Util.getIntValue(language, 7));
            }
            resultStr = resultStr.startsWith("、") ? resultStr.substring(1) : resultStr;
            resultStr = SystemEnv.getHtmlLabelName(506905, Util.getIntValue(language, 7)) + resultStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultStr;
    }

    public String getDsporder(String dsporder){
        BigDecimal bigDecimal = new BigDecimal(dsporder);
        return bigDecimal.toPlainString();
    }
}
