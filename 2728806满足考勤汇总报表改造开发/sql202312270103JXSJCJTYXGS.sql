-- ���ڻ��ܱ����Զ����ǩ  ȱ������
insert into htmllabelinfo(indexid,labelname,languageid) values(-1220001,'ȱ������',7);
insert into htmllabelindex(id,indexdesc) values(-1220001,'ȱ������');

-- ���ڻ��ܱ����Զ����ֶ� ȱ��������֧����ϸ��
Insert into kq_report_field(fieldname,fieldlabel,width,unittype,ISDEFINEDCOLUMN,DEFAULTSHOW,ISLEAVETYPE,REPORTTYPE,ISDATACOLUMN,SHOWDETIAL,SHOWORDER,GROUPID,ISENABLE,ISSYSTEM)
VALUES('absenteeismDays','-1220001',65,1,1,1,0,'month',1,1,28.1,2,1,1);

-- ����detail�������ֶ� ȱ������
alter table KQ_FORMAT_DETAIL
add absenteeismDays int default 0;
-- ����total�������ֶ� ȱ������
alter table KQ_FORMAT_TOTAL
add absenteeismDays int default 0;

-- ���ڻ��ܱ����Զ����ǩ  ��ҹ��
insert into htmllabelinfo(indexid,labelname,languageid) values(-1220002,'��ҹ��',7);
insert into htmllabelindex(id,indexdesc) values(-1220002,'��ҹ��');
-- ���ڻ��ܱ����Զ����ֶ� ��ҹ�֧ࣨ����ϸ��
Insert into kq_report_field(fieldname,fieldlabel,width,unittype,ISDEFINEDCOLUMN,DEFAULTSHOW,ISLEAVETYPE,REPORTTYPE,ISDATACOLUMN,SHOWDETIAL,SHOWORDER,GROUPID,ISENABLE,ISSYSTEM)
VALUES('nightshiftBig','-1220002',65,3,1,1,0,'month',1,1,28.2,2,1,1);
-- ����detail�������ֶ� ��ҹ��
alter table KQ_FORMAT_DETAIL
add nightshiftBig int default 0;
-- ����total�������ֶ� ��ҹ��
alter table KQ_FORMAT_TOTAL
add nightshiftBig int default 0;

-- ���ڻ��ܱ����Զ����ǩ  Сҹ��
insert into htmllabelinfo(indexid,labelname,languageid) values(-1220003,'Сҹ��',7);
insert into htmllabelindex(id,indexdesc) values(-1220003,'Сҹ��');
-- ���ڻ��ܱ����Զ����ֶ� Сҹ�֧ࣨ����ϸ��
Insert into kq_report_field(fieldname,fieldlabel,width,unittype,ISDEFINEDCOLUMN,DEFAULTSHOW,ISLEAVETYPE,REPORTTYPE,ISDATACOLUMN,SHOWDETIAL,SHOWORDER,GROUPID,ISENABLE,ISSYSTEM)
VALUES('nightshiftSmall','-1220003',65,3,1,1,0,'month',1,1,28.3,2,1,1);
-- ����detail�������ֶ� Сҹ��
alter table KQ_FORMAT_DETAIL
    add nightshiftSmall int default 0;
-- ����total�������ֶ� Сҹ��
alter table KQ_FORMAT_TOTAL
    add nightshiftSmall int default 0;

-- ���ڻ��ܱ����Զ����ǩ  ʵ�ʹ����ռӰ�ʱ��
insert into htmllabelinfo(indexid,labelname,languageid) values(-1220004,'ʵ�ʹ����ռӰ�ʱ��',7);
insert into htmllabelindex(id,indexdesc) values(-1220004,'ʵ�ʹ����ռӰ�ʱ��');
-- ���ڻ��ܱ����Զ����ֶ� ʵ�ʹ����ռӰ�ʱ������֧����ϸ��
Insert into kq_report_field(fieldname,fieldlabel,width,unittype,ISDEFINEDCOLUMN,DEFAULTSHOW,ISLEAVETYPE,REPORTTYPE,ISDATACOLUMN,SHOWDETIAL,SHOWORDER,GROUPID,ISENABLE,ISSYSTEM)
VALUES('workOverTimeReal','-1220004',65,2,1,1,0,'month',1,1,28.3,2,1,1);

-- ���ڻ��ܱ����Զ����ǩ  ������֮��Ӱ�����
insert into htmllabelinfo(indexid,labelname,languageid) values(-1220005,'������֮��Ӱ�����',7);
insert into htmllabelindex(id,indexdesc) values(-1220005,'������֮��Ӱ�����');
-- ���ڻ��ܱ����Զ����ֶ� ������֮��Ӱ���������֧����ϸ��
Insert into kq_report_field(fieldname,fieldlabel,width,unittype,ISDEFINEDCOLUMN,DEFAULTSHOW,ISLEAVETYPE,REPORTTYPE,ISDATACOLUMN,SHOWDETIAL,SHOWORDER,GROUPID,ISENABLE,ISSYSTEM)
VALUES('overTimeDaysExtras','-1220005',65,1,1,1,0,'month',1,1,28.3,2,1,1);
/

