package weaver.mobile.sign;

import weaver.conn.RecordSet;
import weaver.general.ThreadVarLanguage;
import weaver.systeminfo.SystemEnv;


public class HrmSign {
	public static String CreateHrmSignSql(String operaterId, String beginQueryDate, String endQueryDate) {
		StringBuilder sql = new StringBuilder();
		RecordSet rs = new RecordSet();
		if(rs.getDBType().equals("oracle")){
			sql.append("select 'hrm'||cast(id as VARCHAR(10)) uniqueid,id,userId as operater,'hrm_sign' as operate_type,signDate as operate_date,signTime as operate_time,LONGITUDE,LATITUDE,ADDR as address,'"+ SystemEnv.getHtmlLabelName(525196, ThreadVarLanguage.getLang())+"' as remark,'' as attachment,signtype,0 as canViewSignImg from HrmScheduleSign ");
		}else if(rs.getDBType().equals("mysql")){
			sql.append("select concat('hrm',convert(id ,char(10))) uniqueid,id,userId as operater,'hrm_sign' as operate_type,signDate as operate_date,signTime as operate_time,LONGITUDE,LATITUDE,ADDR as address,'"+ SystemEnv.getHtmlLabelName(525196, ThreadVarLanguage.getLang())+"' as remark,'' as attachment,signtype,0 as canViewSignImg from HrmScheduleSign ");
		}
		else if(rs.getDBType().equals("postgresql")){
			sql.append("select 'hrm'||cast(id as VARCHAR(10)) uniqueid,id,userId as operater,'hrm_sign' as operate_type,signDate as operate_date,signTime as operate_time,LONGITUDE,LATITUDE,ADDR as address,'"+ SystemEnv.getHtmlLabelName(525196, ThreadVarLanguage.getLang())+"' as remark,'' as attachment,signtype,0 as canViewSignImg from HrmScheduleSign ");
		}
		else{
			sql.append("select 'hrm'+cast(id as VARCHAR(10)) uniqueid,id,userId as operater,'hrm_sign' as operate_type,signDate as operate_date,signTime as operate_time,LONGITUDE,LATITUDE,ADDR as address,'"+ SystemEnv.getHtmlLabelName(525196, ThreadVarLanguage.getLang())+"' as remark,'' as attachment,signtype,0 as canViewSignImg from HrmScheduleSign ");
		}
		sql.append("where isInCom='1' and userId in (").append(operaterId).append(") ");
		if(rs.getDBType().equals("oracle")){
			if(!"".equals(beginQueryDate)){
				sql.append(" AND signDate||' '||signTime>='").append(beginQueryDate).append("' ");
			}
			if(!"".equals(endQueryDate)){
				sql.append(" AND signDate||' '||signTime<='").append(endQueryDate).append("' ");
			}
			sql.append(" AND LONGITUDE is not null and LATITUDE is not null ");
		}else if(rs.getDBType().equals("mysql")){
			if(!"".equals(beginQueryDate)){
				sql.append(" AND concat(signDate,' ',signTime)>='").append(beginQueryDate).append("' ");
			}
			if(!"".equals(endQueryDate)){
				sql.append(" AND concat(signDate,' ',signTime)<='").append(endQueryDate).append("' ");
			}
			sql.append(" AND LONGITUDE is not null and LATITUDE is not null ");
		}
		else if(rs.getDBType().equals("postgresql")){
			if(!"".equals(beginQueryDate)){
				sql.append(" AND signDate||' '||signTime>='").append(beginQueryDate).append("' ");
			}
			if(!"".equals(endQueryDate)){
				sql.append(" AND signDate||' '||signTime<='").append(endQueryDate).append("' ");
			}
			sql.append(" AND LONGITUDE is not null and LATITUDE is not null ");
		}
		else{
			if(!"".equals(beginQueryDate)){
				sql.append(" AND signDate+' '+signTime>='").append(beginQueryDate).append("' ");
			}
			if(!"".equals(endQueryDate)){
				sql.append(" AND signDate+' '+signTime<='").append(endQueryDate).append("' ");
			}
			sql.append(" AND LONGITUDE!='' and LATITUDE!='' ");
		}
		
		return sql.toString();
	}

  /**
   * 不包含由外勤签到生成的考勤数据
   * @param operaterId
   * @param beginQueryDate
   * @param endQueryDate
   * @return
   */
	public static String CreateHrmSignSql4E9(String operaterId, String beginQueryDate, String endQueryDate) {
		StringBuilder sql = new StringBuilder();
		RecordSet rs = new RecordSet();
		if(rs.getDBType().equals("oracle")){
			sql.append("select 'hrm'||cast(id as VARCHAR(10)) uniqueid,id,userId as operater,'hrm_sign' as operate_type,signDate as operate_date,signTime as operate_time,LONGITUDE,LATITUDE,ADDR as address,'"+ SystemEnv.getHtmlLabelName(525196, ThreadVarLanguage.getLang())+"' as remark,'' as attachment,signtype,'hrm'||cast(id as VARCHAR(10)) as canViewSignImg, null as crm from HrmScheduleSign ");
		}else if(rs.getDBType().equals("mysql")){
			sql.append("select concat('hrm',convert(id ,char(10))) uniqueid,id,userId as operater,'hrm_sign' as operate_type,signDate as operate_date,signTime as operate_time,LONGITUDE,LATITUDE,ADDR as address,'"+ SystemEnv.getHtmlLabelName(525196, ThreadVarLanguage.getLang())+"' as remark,'' as attachment,signtype,concat('hrm',convert(id ,char(10))) as canViewSignImg, null as crm from HrmScheduleSign ");
		}else{
			//qc2535208
			sql.append("select 'hrm'+cast(id as VARCHAR(10)) uniqueid,id,userId as operater,'hrm_sign' as operate_type,signDate as operate_date,signTime as operate_time,LONGITUDE,LATITUDE,ADDR as address,"/*+ SystemEnv.getHtmlLabelName(525196, ThreadVarLanguage.getLang())*/+" remark, attachment,signtype,'hrm'+cast(id as VARCHAR(10)) as canViewSignImg, null as crm from HrmScheduleSign ");
		}
		sql.append("where isInCom='1' and (signfrom <> 'e9_mobile_out' and signfrom <> 'EMSyn_out' and signfrom <> 'Wechat_out') and userId in (").append(operaterId).append(") ");
		if(rs.getDBType().equals("oracle")){
			if(!"".equals(beginQueryDate)){
				sql.append(" AND signDate||' '||signTime>='").append(beginQueryDate).append("' ");
			}
			if(!"".equals(endQueryDate)){
				sql.append(" AND signDate||' '||signTime<='").append(endQueryDate).append("' ");
			}
			sql.append(" AND LONGITUDE is not null and LATITUDE is not null ");
		}
		else if(rs.getDBType().equals("postgresql")){
			if(!"".equals(beginQueryDate)){
				sql.append(" AND signDate||' '||signTime>='").append(beginQueryDate).append("' ");
			}
			if(!"".equals(endQueryDate)){
				sql.append(" AND signDate||' '||signTime<='").append(endQueryDate).append("' ");
			}
			sql.append(" AND LONGITUDE is not null and LATITUDE is not null ");
		}
		else if(rs.getDBType().equals("mysql")){
			if(!"".equals(beginQueryDate)){
				sql.append(" AND concat(signDate,' ',signTime)>='").append(beginQueryDate).append("' ");
			}
			if(!"".equals(endQueryDate)){
				sql.append(" AND concat(signDate,' ',signTime)<='").append(endQueryDate).append("' ");
			}
			sql.append(" AND LONGITUDE is not null and LATITUDE is not null ");
		}else{
			if(!"".equals(beginQueryDate)){
				sql.append(" AND signDate+' '+signTime>='").append(beginQueryDate).append("' ");
			}
			if(!"".equals(endQueryDate)){
				sql.append(" AND signDate+' '+signTime<='").append(endQueryDate).append("' ");
			}
			sql.append(" AND LONGITUDE!='' and LATITUDE!='' ");
		}

		return sql.toString();
	}
}
