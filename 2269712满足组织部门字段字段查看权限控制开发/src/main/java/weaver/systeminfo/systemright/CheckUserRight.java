package weaver.systeminfo.systemright;

import com.alibaba.fastjson.JSON;
import javafx.beans.binding.ObjectExpression;
import weaver.general.BaseBean;
import weaver.conn.RecordSet;
import weaver.general.StaticObj;
import weaver.general.Util;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.User;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import weaver.systeminfo.systemright.CheckSubCompanyRight;

/**
 * Title:        cobusiness
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:      weaver
 * @author liuyu
 * @version 1.0
 */

public class CheckUserRight  extends BaseBean {

  //private RecordSet rt = null;
  private StaticObj staticobj = null;
  private String memberlevel ;
  private static Object lock = new Object();

  public CheckUserRight() {
    staticobj = StaticObj.getInstance();
    memberlevel = "0" ;
    getRightInfo() ;
  }

  private void getRightInfo()  {
    synchronized (lock) {
      if (staticobj.getObject("Role&Rightdetail") == null || ((Hashtable) staticobj.getObject("Role&Rightdetail")).size() == 0)
        setRoleRightdetailInfo();
      if (staticobj.getObject("Member&Role") == null || ((Hashtable) staticobj.getObject("Member&Role")).size() == 0)
        setMemberRoleInfo();
    }
  }

  private void setRoleRightdetailInfo() {

    RecordSet rt = new RecordSet() ;
	ArrayList tempright = new ArrayList() ;  
	ArrayList templevel = new ArrayList() ;
    try{
      rt.executeProc("RoleRightdetailInfo_Select","") ;
      String roleidold="";
      while(rt.next()){
        String roleid = rt.getString(1) ;
        String rightdetail = rt.getString(2).toLowerCase() ;
        String rolelevel = rt.getString(3) ;
        //ArrayList tempright = (ArrayList)staticobj.getRecordFromObj("Role&Rightdetail",roleid+"_right") ;
        //ArrayList templevel = (ArrayList)staticobj.getRecordFromObj("Role&Rightdetail",roleid+"_level") ;
       if(!roleid.equals(roleidold)||roleidold.equals("")) {
         tempright = new ArrayList() ;  templevel = new ArrayList() ;
         staticobj.putRecordToObj("Role&Rightdetail",roleid+"_right",tempright);
         staticobj.putRecordToObj("Role&Rightdetail",roleid+"_level",templevel);
       }
        tempright.add(rightdetail) ;
        templevel.add(rolelevel) ;
        roleidold=rt.getString(1) ;
      }
     
    }
    catch(Exception e) {
      writeLog(e) ;
    }
  }

  private void setMemberRoleInfoBak() {

    RecordSet rt = new RecordSet() ;
    ArrayList temprole = new ArrayList() ;  
	ArrayList templevel = new ArrayList() ;
	try{
	String resourceidold="";
      rt.executeProc("MemberRoleInfo_Select","") ;
      while(rt.next()){
        String resourceid = rt.getString(1) ;
        String roleid = rt.getString(2) ;
        String rolelevel = rt.getString(3) ;
if(!resourceid.equals(resourceidold)||resourceidold.equals("")) {
	temprole = new ArrayList() ;  
	templevel = new ArrayList() ;
 	staticobj.putRecordToObj("Member&Role",resourceid+"_role"+"1",temprole);
        staticobj.putRecordToObj("Member&Role",resourceid+"_level"+"1",templevel);
     }
        temprole.add(roleid) ;
        templevel.add(rolelevel) ;
	resourceidold=rt.getString(1) ;
      }
	  
    }
    catch(Exception e) {
      writeLog(e) ;
    }
  }
	
  private void setMemberRoleInfo() {

    RecordSet rt = new RecordSet() ;
    ArrayList temprole = new ArrayList() ;  
	ArrayList templevel = new ArrayList() ;
	try{
	String resourceidold="";
	String sql = "";

	String jobtitleSql = "";
  if ("oracle".equals(rt.getDBType())) {
    jobtitleSql = " WHERE  (a.jobtitle = b.resourceid AND b.resourcetype=5 AND (b.jobtitlelevel=1 OR (b.jobtitlelevel=2 AND ','||b.subdepid ||',' LIKE  '%,'||a.subcompanyid1||',%') OR (b.jobtitlelevel=3 AND ','||b.subdepid||',' LIKE '%,' || a.departmentid ||',%')))";
  }else if ("sqlserver".equals(rt.getDBType())) {
    jobtitleSql = " WHERE  (a.jobtitle = b.resourceid AND b.resourcetype=5 AND (b.jobtitlelevel=1 OR (b.jobtitlelevel=2 AND ','+cast(b.subdepid as varchar) +',' LIKE  '%,'+ cast(a.subcompanyid1 as varchar)+',%') OR (b.jobtitlelevel=3 AND ','+cast(b.subdepid as varchar)+',' LIKE '%,' +  cast(a.departmentid as varchar) +',%')))";
  }else if ("mysql".equals(rt.getDBType())) {
    jobtitleSql = " WHERE  (a.jobtitle = b.resourceid AND b.resourcetype=5 AND (b.jobtitlelevel=1 OR (b.jobtitlelevel=2 AND CONCAT(',',b.subdepid ,',') LIKE  CONCAT('%,',a.subcompanyid1,',%')) OR (b.jobtitlelevel=3 AND CONCAT(',',b.subdepid ,',') LIKE  CONCAT('%,',a.departmentid,',%'))))";
  }

	sql = " SELECT distinct resourceid, roleid , rolelevel FROM ( "+
				" SELECT a.id AS resourceid, b.roleid , b.rolelevel, a.status FROM HrmResource a, HrmRoleMembers b "+
				" WHERE (a.id=b.resourceid and b.resourcetype =1 ) "+
				" UNION ALL "+
				" SELECT a.id AS resourceid, b.roleid , b.rolelevel, a.status FROM HrmResourceManager a, HrmRoleMembers b "+
				" WHERE (a.id=b.resourceid and b.resourcetype IN(7,8)) "+
				" UNION ALL "+
				" SELECT a.id AS resourceid, b.roleid , b.rolelevel, a.status FROM HrmResource a, HrmRoleMembers b "+
				" WHERE (a.subcompanyid1 = b.resourceid AND a.seclevel>=b.seclevelfrom AND a.seclevel<=b.seclevelto AND b.resourcetype=2) "+
				" UNION ALL "+
				" SELECT a.id AS resourceid, b.roleid , b.rolelevel, a.status FROM HrmResource a, HrmRoleMembers b "+
				" WHERE (a.departmentid = b.resourceid AND a.seclevel>=b.seclevelfrom AND a.seclevel<=b.seclevelto AND b.resourcetype=3) "+
				" UNION ALL "+
				" SELECT a.id AS resourceid, b.roleid , b.rolelevel, a.status FROM HrmResource a, HrmRoleMembers b "+
				//" WHERE  (a.jobtitle = b.resourceid AND b.resourcetype=5 AND (b.jobtitlelevel=1 OR (b.jobtitlelevel=2 AND CONCAT(',',b.subdepid ,',') LIKE  CONCAT('%,',a.subcompanyid1,',%')) OR (b.jobtitlelevel=3 AND CONCAT(',',b.subdepid ,',') LIKE  CONCAT('%,',a.departmentid,',%'))))" +
        jobtitleSql +
        " ) t" +
				" where t.status in (0,1,2,3) "+
				" ORDER BY resourceid";
      rt.executeQuery(sql) ;
      while(rt.next()){
        String resourceid = rt.getString(1) ;
        String roleid = rt.getString(2) ;
        String rolelevel = rt.getString(3) ;
if(!resourceid.equals(resourceidold)||resourceidold.equals("")) {
	temprole = new ArrayList() ;  
	templevel = new ArrayList() ;
 	staticobj.putRecordToObj("Member&Role",resourceid+"_role"+"1",temprole);
        staticobj.putRecordToObj("Member&Role",resourceid+"_level"+"1",templevel);
     }
        temprole.add(roleid) ;
        templevel.add(rolelevel) ;
	resourceidold=rt.getString(1) ;
      }
	  
    }
    catch(Exception e) {
      writeLog(e) ;
    }
  }

  public boolean checkUserRight(String processname, User user) {
    String userid = ""+user.getUID() ;
	String usertype = ""+user.getLogintype() ;
    boolean hastheright = false ;
    ArrayList temprole = (ArrayList)staticobj.getRecordFromObj("Member&Role",userid+"_role"+usertype) ;     //获取该用户的角色id
    new BaseBean().writeLog("==zj==(temprole)" + JSON.toJSONString(temprole));
    ArrayList templevel = (ArrayList)staticobj.getRecordFromObj("Member&Role",userid+"_level"+usertype) ;
    if(temprole == null) return false ;
    for(int i=0 ; i< temprole.size() ; i++)  {
      String roleid = Util.null2String((String)temprole.get(i)) ;
      String rolelevel = Util.null2String((String)templevel.get(i)) ;
      ArrayList tempright = (ArrayList)staticobj.getRecordFromObj("Role&Rightdetail",roleid+"_right") ;
      new BaseBean().writeLog("==zj==(tempright)" + JSON.toJSONString(tempright));
      ArrayList templevelinright = (ArrayList)staticobj.getRecordFromObj("Role&Rightdetail",roleid+"_level") ;
      new BaseBean().writeLog("==zj==(templevelinright)" + JSON.toJSONString(templevelinright));
      if(tempright == null) continue ;
      int index = tempright.indexOf(processname.toLowerCase()) ;
      if(index == -1) continue ;
      String rightlevel = (String)templevelinright.get(index) ;
      if(rolelevel.compareTo(rightlevel) < 0 ) continue ;
      hastheright = true ;
      if(rolelevel.compareTo(memberlevel) > 0) memberlevel = rolelevel ;
    }
    return hastheright ;
  }

  public boolean checkUserRight(String processname, User user, String departmentid) {
    if(!checkUserRight(processname,user)) return false ;
    if(memberlevel.equals("2")) return true ;
    if(departmentid.equals(""+user.getUserDepartment())) return true ;

    try {
        DepartmentComInfo dc = new DepartmentComInfo() ;
        String subcompany1 = dc.getSubcompanyid1(departmentid) ;
        String usersubcompany1 = "" + user.getUserSubCompany1() ;
   	
	    if(memberlevel.equals("1")) {
	        if(subcompany1.equals(usersubcompany1))
	        	return true ;
	    }

	    CheckSubCompanyRight cs=new CheckSubCompanyRight();
	    if(cs.ChkComRightByUserRightCompanyId(user.getUID(),processname,Integer.parseInt(subcompany1))>0) 
	    	return true;	    
    }catch(Exception ex) {}
    
    return false ;
  }

  public String getRightLevel(String processname, User user) {
    if(!checkUserRight(processname,user)) return "-1" ;
    else return memberlevel ;
  }

  public String getRightLevel(String resourceid , String roleid) {
	ArrayList temprole = (ArrayList)staticobj.getRecordFromObj("Member&Role",resourceid+"_role"+"1") ; //由于有第一层checkUserRight判断，故这里可以默认为内部用户
    ArrayList templevel = (ArrayList)staticobj.getRecordFromObj("Member&Role",resourceid+"_level"+"1") ;
    if(temprole == null) return "-1" ;
	int index = temprole.indexOf(roleid) ;
	if(index == -1) return "-1" ;
	return (String)templevel.get(index) ;
  }

  public boolean checkUserRight(String resourceid , String roleid , String rolelevel) {
	String tmprolelevel = getRightLevel(resourceid , roleid) ;
	if(tmprolelevel.equals("-1")) return false ;
	if(tmprolelevel.compareTo(rolelevel) < 0) return false ;
	return true ;
  }


  public boolean checkUserRole(String roleid , User user, String departmentid) {
    String resourceid = "" + user.getUID() ;
	String rolelevel = getRightLevel( resourceid , roleid) ;
    if(rolelevel.equals("-1")) return false ; 
    if(rolelevel.equals("2")) return true ;
    if(departmentid.equals(""+user.getUserDepartment())) return true ;
    if(rolelevel.equals("1")) {
      try {
        DepartmentComInfo dc = new DepartmentComInfo() ;
        String subcompany1 = dc.getSubcompanyid1(departmentid) ;
 /*       String subcompany2 = dc.getSubcompanyid2(departmentid) ;
        String subcompany3 = dc.getSubcompanyid3(departmentid) ;
        String subcompany4 = dc.getSubcompanyid4(departmentid) ; */
        String usersubcompany1 = "" + user.getUserSubCompany1() ;
/*        String usersubcompany2 = "" + user.getUserSubCompany2() ;
        String usersubcompany3 = "" + user.getUserSubCompany3() ;
        String usersubcompany4 = "" + user.getUserSubCompany4() ;  */
        if(subcompany1.equals(usersubcompany1) /* &&
           subcompany2.equals(usersubcompany2) &&
           subcompany3.equals(usersubcompany3) &&
           subcompany4.equals(usersubcompany4) */ )  return true ;
      }catch(Exception ex) {}
    }


    return false ;
  }

  
  public void updateMemberRole(String resourceid , String roleid , String level)  {
    ArrayList temprole = (ArrayList)staticobj.getRecordFromObj("Member&Role",resourceid+"_role"+"1") ;   
    ArrayList templevel = (ArrayList)staticobj.getRecordFromObj("Member&Role",resourceid+"_level"+"1") ;
    if( temprole == null) {
      temprole = new ArrayList() ;  templevel = new ArrayList() ;
      staticobj.putRecordToObj("Member&Role",resourceid+"_role"+"1",temprole);
      staticobj.putRecordToObj("Member&Role",resourceid+"_level"+"1",templevel);
      temprole.add(roleid) ; templevel.add(level) ;
      return ;
    }
    int index = temprole.indexOf(roleid) ;
    if(index == -1) {
      temprole.add(roleid) ; templevel.add(level) ;
    }
    else {
      templevel.set(index,level) ;
    }
  }

  public void deleteMemberRole(String resourceid , String roleid)  {
    ArrayList temprole = (ArrayList)staticobj.getRecordFromObj("Member&Role",resourceid+"_role"+"1") ;
    ArrayList templevel = (ArrayList)staticobj.getRecordFromObj("Member&Role",resourceid+"_level"+"1") ;
    if( temprole == null) return ;
    int index = temprole.indexOf(roleid) ;
    if(index == -1) return ;
    temprole.remove(index) ;
    templevel.remove(index) ;
  }


  public void updateRoleRightdetail(String roleid , String rightid , String level)  {
    ArrayList tempright = (ArrayList)staticobj.getRecordFromObj("Role&Rightdetail",roleid+"_right") ;
    ArrayList templevel = (ArrayList)staticobj.getRecordFromObj("Role&Rightdetail",roleid+"_level") ;
	if( tempright == null) {
      tempright = new ArrayList() ;  templevel = new ArrayList() ;
    }
	RecordSet rt = new RecordSet() ;
    rt.executeProc("SystemRightDetail_SByRightID",rightid) ;
    while(rt.next()) {
		String temprightdetail = rt.getString("rightdetail").toLowerCase() ;
		int index = tempright.indexOf(temprightdetail) ;
		if(index == -1) {
		  tempright.add(temprightdetail) ; templevel.add(level) ;
		} else {
		  templevel.set(index,level) ;
		}
	 }
    staticobj.putRecordToObj("Role&Rightdetail",roleid+"_right",tempright);
    staticobj.putRecordToObj("Role&Rightdetail",roleid+"_level",templevel);
  }

  public void deleteRoleRightdetail(String roleid , String rightid)  {
    ArrayList tempright = (ArrayList)staticobj.getRecordFromObj("Role&Rightdetail",roleid+"_right") ;
    ArrayList templevel = (ArrayList)staticobj.getRecordFromObj("Role&Rightdetail",roleid+"_level") ;
    if( tempright == null) return ;
	RecordSet rt = new RecordSet() ;
    rt.executeProc("SystemRightDetail_SByRightID",rightid) ;
    while(rt.next()) {
		String temprightdetail = rt.getString("rightdetail").toLowerCase() ;
		int index = tempright.indexOf(temprightdetail) ;
		if(index == -1) continue ;
		tempright.remove(index) ;
		templevel.remove(index) ;
	}
    staticobj.putRecordToObj("Role&Rightdetail",roleid+"_right",tempright);
    staticobj.putRecordToObj("Role&Rightdetail",roleid+"_level",templevel);
  }

  public void removeRoleRightdetailCache(){
    staticobj.removeObject("Role&Rightdetail");
    staticobj.removeObject("mapHrmRoleSR");
  }

  public void removeMemberRoleCache(){
    staticobj.removeObject("Member&Role");
    staticobj.removeObject("mapHrmRoleSR");
  }
  
  public ArrayList getMemberRoleByResourceid(String resourceid){
  	return (ArrayList)staticobj.getRecordFromObj("Member&Role",resourceid+"_role1") ;
  }
  
  public ArrayList getMemberRoleLevelByResourceid(String resourceid){
  	return (ArrayList)staticobj.getRecordFromObj("Member&Role",resourceid+"_level1") ;
  }

}