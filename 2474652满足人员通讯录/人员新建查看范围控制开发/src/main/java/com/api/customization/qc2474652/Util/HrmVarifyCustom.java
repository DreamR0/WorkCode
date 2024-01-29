package com.api.customization.qc2474652.Util;

import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;

import java.util.ArrayList;

public class HrmVarifyCustom {
private String tableName = new BaseBean().getPropValue("qc2474652","tableName");

    /**
     * 判断人员是否有权限新建人员或者通讯录导出
     * @param user
     * @return
     */
    public Boolean checkUserRight(User user){
        RecordSet rs = new RecordSet();
        String sql = "";
        Boolean isHave = false;
        if (user.getUID() == 1)return true;     //如果为管理员则直接返回true
        try{
            sql  = "select * from "+tableName+" where  FIND_IN_SET('"+user.getUID()+"',ry)";
            new BaseBean().writeLog("==zj==(该人员是否在建模表)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
           if (rs.next()){
               isHave = true;
           }
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(该人员是否在建模表异常)" + e);
        }

        return isHave;
    }

    /**
     * 获取人员对应分部权限
     * @param user
     * @return
     */
    public String[]  getSubCompany(User user){
        String ids = "";
        String[] depIds = null;
        String[] subSplit = null;
        String subIds = "";
        String subName = "";
        RecordSet rs = new RecordSet();
        String sql ="";
        try {
            //获取人员对应建模表中的部门id
            sql = "select * from "+tableName+" where  FIND_IN_SET('"+user.getUID()+"',ry)";
            rs.executeQuery(sql);
            while (rs.next()){
                //防止出现多条设置的情况
                 ids += Util.null2String(rs.getString("bm")) + ",";
                 depIds = ids.split(",");
            }
            //根据部门id获取所属分部
            for (int i = 0; i < depIds.length; i++) {
                sql = "select * from hrmdepartment where id = "+depIds[i];
                rs.executeQuery(sql);
                while (rs.next()){
                    subIds += Util.null2String(rs.getString("subcompanyid1"))+",";
                }
            }
            subSplit = subIds.split(",");
            //根据分部id获取全部上级分部名称
            for (int i = 0; i < subSplit.length; i++) {
                subName += new SubCompanyComInfo().getSubcompanynames(subSplit[i]) +",";
            }
            subSplit = subName.split(",");

        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(部门权限错误)" + JSON.toJSONString(e));
        }
        return subSplit;
    }

    /**
     * 获取人员对应部门权限--浏览按钮
     * @param user
     * @return
     */
    public ArrayList<String> getDepartment(User user){
        RecordSet rs = new RecordSet();
        String sql ="";
        String ids = "";
        String[] depIds = null;     //建模维护部门
        String SupDePId = "";       //建模维护部门的上级部门
        String[] SupDePIds = null;       //建模维护部门的上级部门
        ArrayList<String> depList = new ArrayList<>();

        try {
            sql = "select * from "+tableName+" where  FIND_IN_SET('"+user.getUID()+"',ry)";
            rs.executeQuery(sql);
            while (rs.next()){
                ids += Util.null2String(rs.getString("bm")) +",";
                depIds = ids.split(",");
            }
            if (depIds.length <= 0 || depIds == null){
                return depList;
            }
            //这里获取该人员维护部门的全部上级部门
            for (int i = 0; i < depIds.length; i++) {
                SupDePId += new DepartmentComInfo().getAllSupDepartment(depIds[i])+",";
            }
            //把本部门也加进去
            SupDePId += ids;
            //将获取到的所有上级部门进行封装
            SupDePIds = SupDePId.split(",");
            //将上级部门封装到list中
            for (int i = 0; i < SupDePIds.length; i++) {
                depList.add(SupDePIds[i]);
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(部门过滤报错)" + JSON.toJSONString(e));
        }

        return depList;
    }

    /**
     * 获取建模表维护部门
     * @param user
     * @param isBrowser
     * @return
     */
    public String getDepartment(User user,Boolean isBrowser){
        RecordSet rs = new RecordSet();
        String sql ="";
        String ids = "";
        try {
            sql = "select * from "+tableName+" where  FIND_IN_SET('"+user.getUID()+"',ry)";
            rs.executeQuery(sql);
            while (rs.next()){
                 ids += Util.null2String(rs.getString("bm")) + ",";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(ids.length() > 0){
            ids = ids.substring(0,ids.length()-1);
        }
        return ids;
    }

    /**
     * 获取分部组织结构树
     * @param user
     * @return
     */
    public String[]  getSubCompanyTree(User user){
        String ids = "";
        String[] depIds = null;
        String[] subSplit = null;
        String subIds = "";     //分部id
        String supSubIds= "";   //分部上级id
        RecordSet rs = new RecordSet();
        String sql ="";
        try {
            //获取人员对应建模表中的部门id
            sql = "select * from "+tableName+" where  FIND_IN_SET('"+user.getUID()+"',ry)";
            rs.executeQuery(sql);
            while (rs.next()){
                 ids += Util.null2String(rs.getString("bm"))+",";
                depIds = ids.split(",");
            }
            if (depIds == null || depIds.length <=0){
                return subSplit;
            }
            //根据部门id获取所属分部
            for (int i = 0; i < depIds.length; i++) {
                sql = "select * from hrmdepartment where id = "+depIds[i];
                rs.executeQuery(sql);
                while (rs.next()){
                    subIds += Util.null2String(rs.getString("subcompanyid1"))+",";
                }
            }
            subSplit = subIds.split(",");
            //根据分部id获取全部上级分部名称
            for (int i = 0; i < subSplit.length; i++) {
                supSubIds += new SubCompanyComInfo().getAllSupCompany(subSplit[i]) +",";
            }
            //上级id和本身分部id拼接
            subIds  += supSubIds;
            subSplit = subIds.split(",");
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(部门权限错误)" + JSON.toJSONString(e));
        }
        return subSplit;
    }

    /**
     * 判断当前部门下是否有子部门，且子部门在建模表中
     * @param user
     * @param isDep
     * @return
     */
    public Boolean isParent(User user,String depId,Boolean isDep)  {
        Boolean isParent = false;
        String idsDep = "";
        String idsDepChild = "";
        try {
            if (isDep){
                //获取当部门下的全部子部门
                idsDepChild = DepartmentComInfo.getAllChildDepartId(depId, idsDepChild);
                //如果没有子部门，返回false
                if (idsDepChild.length() <= 0 || idsDepChild == null){
                    return isParent;
                }
                String[] splitDep = idsDepChild.split(",");
                //如果有子部门在建模表中，返回true
                idsDep = getDepartment(user, false);
                for (int i = 0; i < splitDep.length; i++) {
                    if (idsDep.contains(splitDep[i])){
                        isParent = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(判断当前部门是否有维护的子部门报错)" + JSON.toJSONString(e));
        }
        return isParent;
    }
}


