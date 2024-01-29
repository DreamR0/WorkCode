-- 考勤汇总报表自定义标签  缺勤天数
insert into htmllabelinfo(indexid,labelname,languageid) values(-1220001,'缺勤天数',7);
insert into htmllabelindex(id,indexdesc) values(-1220001,'缺勤天数');

-- 考勤汇总报表自定义字段 缺勤天数（支持明细）
Insert into kq_report_field(fieldname,fieldlabel,width,unittype,ISDEFINEDCOLUMN,DEFAULTSHOW,ISLEAVETYPE,REPORTTYPE,ISDATACOLUMN,SHOWDETIAL,SHOWORDER,GROUPID,ISENABLE,ISSYSTEM)
VALUES('absenteeismDays','-1220001',65,1,1,1,0,'month',1,1,28.1,2,1,1);

-- 考勤detail表增加字段 缺勤天数
alter table KQ_FORMAT_DETAIL
add absenteeismDays int default 0;
-- 考勤total表增加字段 缺勤天数
alter table KQ_FORMAT_TOTAL
add absenteeismDays int default 0;

-- 考勤汇总报表自定义标签  大夜班
insert into htmllabelinfo(indexid,labelname,languageid) values(-1220002,'大夜班',7);
insert into htmllabelindex(id,indexdesc) values(-1220002,'大夜班');
-- 考勤汇总报表自定义字段 大夜班（支持明细）
Insert into kq_report_field(fieldname,fieldlabel,width,unittype,ISDEFINEDCOLUMN,DEFAULTSHOW,ISLEAVETYPE,REPORTTYPE,ISDATACOLUMN,SHOWDETIAL,SHOWORDER,GROUPID,ISENABLE,ISSYSTEM)
VALUES('nightshiftBig','-1220002',65,3,1,1,0,'month',1,1,28.2,2,1,1);
-- 考勤detail表增加字段 大夜班
alter table KQ_FORMAT_DETAIL
add nightshiftBig int default 0;
-- 考勤total表增加字段 大夜班
alter table KQ_FORMAT_TOTAL
add nightshiftBig int default 0;

-- 考勤汇总报表自定义标签  小夜班
insert into htmllabelinfo(indexid,labelname,languageid) values(-1220003,'小夜班',7);
insert into htmllabelindex(id,indexdesc) values(-1220003,'小夜班');
-- 考勤汇总报表自定义字段 小夜班（支持明细）
Insert into kq_report_field(fieldname,fieldlabel,width,unittype,ISDEFINEDCOLUMN,DEFAULTSHOW,ISLEAVETYPE,REPORTTYPE,ISDATACOLUMN,SHOWDETIAL,SHOWORDER,GROUPID,ISENABLE,ISSYSTEM)
VALUES('nightshiftSmall','-1220003',65,3,1,1,0,'month',1,1,28.3,2,1,1);
-- 考勤detail表增加字段 小夜班
alter table KQ_FORMAT_DETAIL
    add nightshiftSmall int default 0;
-- 考勤total表增加字段 小夜班
alter table KQ_FORMAT_TOTAL
    add nightshiftSmall int default 0;

-- 考勤汇总报表自定义标签  实际工作日加班时长
insert into htmllabelinfo(indexid,labelname,languageid) values(-1220004,'实际工作日加班时长',7);
insert into htmllabelindex(id,indexdesc) values(-1220004,'实际工作日加班时长');
-- 考勤汇总报表自定义字段 实际工作日加班时长（不支持明细）
Insert into kq_report_field(fieldname,fieldlabel,width,unittype,ISDEFINEDCOLUMN,DEFAULTSHOW,ISLEAVETYPE,REPORTTYPE,ISDATACOLUMN,SHOWDETIAL,SHOWORDER,GROUPID,ISENABLE,ISSYSTEM)
VALUES('workOverTimeReal','-1220004',65,2,1,1,0,'month',1,1,28.3,2,1,1);

-- 考勤汇总报表自定义标签  工作日之外加班天数
insert into htmllabelinfo(indexid,labelname,languageid) values(-1220005,'工作日之外加班天数',7);
insert into htmllabelindex(id,indexdesc) values(-1220005,'工作日之外加班天数');
-- 考勤汇总报表自定义字段 工作日之外加班天数（不支持明细）
Insert into kq_report_field(fieldname,fieldlabel,width,unittype,ISDEFINEDCOLUMN,DEFAULTSHOW,ISLEAVETYPE,REPORTTYPE,ISDATACOLUMN,SHOWDETIAL,SHOWORDER,GROUPID,ISENABLE,ISSYSTEM)
VALUES('overTimeDaysExtras','-1220005',65,1,1,1,0,'month',1,1,28.3,2,1,1);
/

