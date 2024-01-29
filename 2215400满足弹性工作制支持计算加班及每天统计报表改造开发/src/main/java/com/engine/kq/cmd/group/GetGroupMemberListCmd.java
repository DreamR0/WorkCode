package com.engine.kq.cmd.group;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQGroupBiz;
import com.engine.kq.biz.KQGroupComInfo;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.util.PageUidFactory;
import com.engine.personalIncomeTax.util.PITUtil;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.companyvirtual.DepartmentVirtualComInfo;
import weaver.hrm.companyvirtual.SubCompanyVirtualComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 获取考勤组管理tab页签
 *
 * @author pzy
 */
public class GetGroupMemberListCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetGroupMemberListCmd(Map<String, Object> params, User user) {
        this.user = user;
        this.params = params;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        Map<String, List<String>> groupMemberMap = new HashMap<>();
        RecordSet rs = new RecordSet();
        try {
            //必要的权限判断
            if (!HrmUserVarify.checkUserRight("HrmKQGroup:Add", user)) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
                return retmap;
            }

            //数据范围参数
            String viewScope = Util.null2String(params.get("viewScope"));
            String subcompanyId = Util.null2String(params.get("subcompanyId"));
            String departmentId = Util.null2String(params.get("departmentId"));
            String resourceId = Util.null2String(params.get("resourceId"));
            String allLevel = Util.null2String(params.get("allLevel"));
            //所属考勤组参数
            String groupType = Util.null2String(params.get("groupType"));
            String groupId = Util.null2String(params.get("groupId"));
            //分页参数
            int pageIndex = Util.getIntValue(Util.null2String(params.get("pageIndex")), 1);
            int pageSize = Util.getIntValue(Util.null2String(params.get("pageSize")), 10);

            String orderBySql = "";
            String orderParams = Util.null2String(params.get("orderParams"));
            if(!"".equals(orderParams)) {
                JSONArray records = JSON.parseArray(orderParams);
                List<List<Object>> params = new ArrayList<List<Object>>();
                for (int i = 0; i < records.size(); i++) {
                    JSONObject r = (JSONObject) records.get(i);
                    String orderKey = Util.null2String(r.get("orderKey"));
                    String orderType = Util.null2String(r.get("orderType"));
                    if ("".equals(orderBySql))
                        orderBySql += " order by " + orderKey + " " + orderType;
                    else
                        orderBySql += ", " + orderKey + " " + orderType;
                }
            }

            int total = 0;

            String groupMemberSql = "";
            String sqlWhere = "";

            ResourceComInfo resourceComInfo = new ResourceComInfo();
            SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
            SubCompanyVirtualComInfo subCompanyVirtualComInfo = new SubCompanyVirtualComInfo();
            DepartmentComInfo departmentComInfo = new DepartmentComInfo();
            DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();
            CheckSubCompanyRight checkSubCompanyRight = new CheckSubCompanyRight();
            ManageDetachComInfo manageDetachComInfo = new ManageDetachComInfo();
            KQGroupBiz kqGroupBiz = new KQGroupBiz();
            KQGroupComInfo kqGroupComInfo = new KQGroupComInfo();
            KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
			Map<String, Object> groupParams = new HashMap<>();

            boolean hrmdetachable = manageDetachComInfo.isUseHrmManageDetach();//是否开启了人力资源模块的管理分权
            if (hrmdetachable) {
                int[] arrSubcompanyids = checkSubCompanyRight.getSubComByUserRightId(user.getUID(), "HrmKQGroup:Add");
                String subcompanyids = "";
                if (user.getUID() != 1) {
                    for (int i = 0; arrSubcompanyids != null && i < arrSubcompanyids.length; i++) {
                        if (subcompanyids.length() > 0) subcompanyids += ",";
                        subcompanyids += arrSubcompanyids[i];
                    }
                    if (subcompanyids.length() > 0) {
                        sqlWhere += " and subcompanyid1 in (" + subcompanyids + ")";
                    } else {
                        sqlWhere += " and 1 = 2 ";
                    }
                }
            }
			if("3".equals(viewScope)){
				groupParams.put("isNoAccount", "1");
			}else{
				sqlWhere += " and status in (0,1,2,3) ";
			}

            switch (viewScope) {
                case "1":
                    if (subcompanyId.length() > 0) {
                        sqlWhere += " and " + Util.getSubINClause(subcompanyId, "subcompanyId1", "IN");
                    }
                    break;
                case "2":
                    if (!"".equals(departmentId)) {
                        sqlWhere += " and " + Util.getSubINClause(departmentId, "departmentid", "IN");
                    }
                    break;
                case "3":
                    if (!"".equals(resourceId)) {
                        sqlWhere += " and " + Util.getSubINClause(resourceId, "id", "IN");
                    }
                    break;
                case "4":
                    if (allLevel.equals("1")) {
                        sqlWhere += " and ( managerStr like '%," + user.getUID() + ",%' )";
                    } else {
                        sqlWhere += " and ( managerid = " + user.getUID() + ")";
                    }
                    break;
            }

            switch (groupType) {
                case "2":
					groupParams.put("groupId", groupId);
                    groupMemberSql = kqGroupBiz.getGroupMemberSql(groupParams);
                    break;
                default:
                    groupMemberSql = kqGroupBiz.getGroupMemberSql(groupParams);
                    break;
            }

            rs.executeQuery(kqGroupBiz.getGroupMemberSql(groupParams));
            while (rs.next()) {
                String id = rs.getString("resourceid");
                String gid = rs.getString("groupid");
                if (!groupMemberMap.containsKey(id))
                    groupMemberMap.put(id, new ArrayList<>());
                groupMemberMap.get(id).add(gid);
            }

//            List<String> resourceIds = new ArrayList<>();
//            for (String key : groupMemberMap.keySet())
//                resourceIds.add(key);

            String inSqlWhere = " and id in (select resourceid from (" + groupMemberSql + ") t1)";// + Util.getSubINClause(String.join(",", resourceIds), "id", "IN");
            String notInSqlWhere = " and id not in (select resourceid from (" + groupMemberSql + ") t1)";//" and " + Util.getSubINClause(String.join(",", resourceIds), "id", "NOT IN");

            //数据列定义
            List columns = new ArrayList();
            Map<String, Object> column = new HashMap();//姓名
            column.put("title", SystemEnv.getHtmlLabelName(25034, Util.getIntValue(user.getLanguage())));
            column.put("dataIndex", "lastname");
            column.put("key", "lastname");
            column.put("width", "15%");
            columns.add(column);

            column = new HashMap();//部门
            column.put("title", SystemEnv.getHtmlLabelName(1933, Util.getIntValue(user.getLanguage())));
            column.put("dataIndex", "workcode");
            column.put("key", "workcode");
            column.put("width", "15%");
            columns.add(column);

            column = new HashMap();//部门
            column.put("title", SystemEnv.getHtmlLabelName(124, Util.getIntValue(user.getLanguage())));
            column.put("dataIndex", "departmentid");
            column.put("key", "departmentid");
            column.put("width", "20%");
            columns.add(column);

            column = new HashMap();//分部
            column.put("title", SystemEnv.getHtmlLabelName(141, Util.getIntValue(user.getLanguage())));
            column.put("dataIndex", "subcompanyid1");
            column.put("key", "subcompanyid1");
            column.put("width", "20%");
            columns.add(column);

            column = new HashMap();//所属考勤组
            column.put("title", SystemEnv.getHtmlLabelName(515132, Util.getIntValue(user.getLanguage())));
            column.put("dataIndex", "groups");
            column.put("key", "groups");
            column.put("width", "25%");
            columns.add(column);

            column = new HashMap();//生效考勤组
            column.put("title", SystemEnv.getHtmlLabelName(509551, Util.getIntValue(user.getLanguage())));
            column.put("dataIndex", "activeGroup");
            column.put("key", "activeGroup");
            column.put("width", "20%");
            columns.add(column);

            //数据源
            List dataSource = new ArrayList();
            String backFields = "id, lastname, subcompanyid1, departmentid,workcode, dsporder";
            String fromSql = " hrmresource ";
            if(groupType.equals("1"))
                sqlWhere += notInSqlWhere;
            else if(groupType.equals("2"))
                sqlWhere += inSqlWhere;
            else{}

            String sql = "select count(1) cnt from " + fromSql + " where 1 = 1 " + sqlWhere;
            writeLog("queryGroupMemberList-sql:"+sql);
            writeLog("queryGroupMemberList-start:" + new Date().getTime());
            rs.executeQuery(sql);
            writeLog("queryGroupMemberList-end:" + new Date().getTime());
            while (rs.next()) {
                total = Util.getIntValue(rs.getString("cnt"), 0);
            }

            sql = backFields + " from " + fromSql + " where 1 = 1 " + sqlWhere;
            String orderBy = ("".equals(orderBySql) ? " order by dsporder" : orderBySql);
            if (pageIndex > 0 && pageSize > 0) {
                if (rs.getDBType().equals("oracle")) {
                    sql = " select * from (select " + sql + ") t " + orderBy;
                    sql = "select * from ( select row_.*, rownum rownum_ from ( " + sql + " ) row_ where rownum <= "
                            + (pageIndex * pageSize) + ") where rownum_ > " + ((pageIndex - 1) * pageSize);
                } else if (rs.getDBType().equals("mysql")) {
                    sql = " select * from (select " + sql + ") t " + orderBy;
                    sql = "select t1.* from (" + sql + ") t1 limit " + ((pageIndex - 1) * pageSize) + "," + pageSize;
                }
                else if (rs.getDBType().equals("postgresql")) {
                    sql = " select * from (select " + sql + ") t " + orderBy;
                    sql = "select t1.* from (" + sql + ") t1 limit " + pageSize + " offset " + ((pageIndex - 1) * pageSize);
                }
                else {
                    int s = 1 + (pageSize * (pageIndex - 1));
                    int e = pageSize * pageIndex;
                    sql = "select " + backFields + " from (select ROW_NUMBER() over(" + orderBy + ") as row, " + sql + ") t where t.row between " + s + " and " + e;
                }
            } else {
                sql = " select " + sql + orderBy;
            }
            writeLog("queryGroupMemberList-sql:"+sql);
            writeLog("queryGroupMemberList1-start:" + new Date().getTime());
            new BaseBean().writeLog("==zj==(考勤组查询)" + sql);
            rs.executeQuery(sql);
            writeLog("queryGroupMemberList1-end:" + new Date().getTime());
            while (rs.next()){
                String id = rs.getString("id");
                String lastname = rs.getString("lastname");
                int subcompanyid1 = rs.getInt("subcompanyid1");
                int departmentid = rs.getInt("departmentid");
                String workcode = Util.null2String(rs.getString("workcode"));
                String subcompanyName = (subcompanyid1 < 0 ? subCompanyVirtualComInfo.getSubCompanyname("" + subcompanyid1) : subCompanyComInfo.getSubcompanyname("" + subcompanyid1));
                String departmentName = (departmentid < 0 ? departmentVirtualComInfo.getDepartmentname("" + subcompanyid1) : departmentComInfo.getDepartmentName("" + departmentid));

                Map<String, Object> data = new HashMap<>();
                data.put("id", id);
                data.put("lastname", lastname);
                data.put("subcompanyid1", subcompanyid1);
                data.put("subcompanyName", subcompanyName);
                data.put("departmentid", departmentid);
                data.put("departmentName", departmentName);
                data.put("workcode", workcode);
                data.put("activeGroupId", "");
                data.put("activeGroup", "");
                if(hrmdetachable){
                    int subCompanyOperatelevel = checkSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"HrmKQGroup:Add", subcompanyid1);
                    data.put("canEdit", subCompanyOperatelevel > 0);
                }else
                    data.put("canEdit", true);
                if(groupMemberMap.containsKey(id)){
                    List<String> groupIds = groupMemberMap.get(id);
                    List<String> groupNames = new ArrayList<>();
                    for(String i : groupIds){
                        String isDelete = kqGroupComInfo.getIsdelete(i);
                        if(!"1".equals(isDelete)) {
                            groupNames.add(kqGroupComInfo.getGroupname(i));
                            String currentGroupId = Util.null2String(kqGroupMemberComInfo.getKQGroupId(id, DateUtil.getCurrentDate()));

                            if (i.equals(currentGroupId)) {
                                data.put("activeGroupId", i);
                                data.put("activeGroup", kqGroupComInfo.getGroupname(i));
                            }
                        }
                    }
                    data.put("groupIds", groupIds);
                    data.put("groups", groupNames);
                }else {
                    data.put("groupIds", new ArrayList<>());
                    data.put("groups", new ArrayList<>());
                }

//                String kqGroupName = groupname;
//                String[] splitStr = Util.splitString(otherPara, "+");
//                String groupid = Util.null2String(splitStr[0]);
//                String resourceid = Util.null2String(splitStr[1]);
//                String kqdate = Util.null2String(splitStr[2]);
//                String strLanguage = Util.null2String(splitStr[3]);
//                if(resourceid.length()>0){
//                    int language = Util.getIntValue(strLanguage,7);
//                    if(kqdate.length()==0) {
//                        kqdate = DateUtil.getCurrentDate();
//                    }
//                    String currentGroupId = Util.null2String(new KQGroupMemberComInfo().getKQGroupId(resourceid,kqdate));
//                    if(groupid.equals(currentGroupId)) {
//                        kqGroupName += "(<span style=\"color:#F00\">"+ SystemEnv.getHtmlLabelName(509551, language)+"</span>)";
//                    }
//                }
//
//                String currentGroupId = Util.null2String(kqGroupMemberComInfo.getKQGroupId(id, ""));
//                data.put("activeGroupId", currentGroupId);
//                data.put("activeGroup", "".equals(currentGroupId) ? "" : kqGroupComInfo.getGroupname(currentGroupId));
                dataSource.add(data);
            }

            retmap.put("columns", columns);
            retmap.put("dataSource", dataSource);
            retmap.put("pageIndex", pageIndex);
            retmap.put("total", total);
            retmap.put("status", "1");
        } catch (Exception e) {
            retmap.put("status", "-1");
            retmap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
            writeLog(e);
        }
        return retmap;
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

}
