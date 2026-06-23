CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `role_desc` varchar(200) DEFAULT NULL COMMENT '角色描述',
  `status` tinyint DEFAULT '1' COMMENT '状态 1正常 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单中间表';
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID 0=顶级',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `menu_icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `menu_path` varchar(100) DEFAULT NULL COMMENT '访问路径',
  `sort_num` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint DEFAULT '1' COMMENT '状态 1正常 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统菜单表';

CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '账号',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '姓名',
  `sys_position` varchar(20) DEFAULT NULL COMMENT '职位：0董事长 1总裁 2副总裁 3处长 4副处长 5技术学术委员会主任 、6副主任 7处长助理 8普通员工',
  `phone` varchar(20) DEFAULT NULL COMMENT '电话',
  `status` tinyint DEFAULT '1' COMMENT '1正常 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门ID 0=顶级',
  `dept_name` varchar(50) NOT NULL COMMENT '部门名称',
  `dept_code` varchar(50) DEFAULT NULL COMMENT '部门编码',
  `dept_level` varchar(20) NOT NULL COMMENT '1：一级；2：二级；3：三级；4：四级',
  `leader` varchar(50) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `sort_num` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint DEFAULT '1' COMMENT '状态 1正常 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';

CREATE TABLE `project_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID（主键）',
  `project_name` varchar(255) NOT NULL COMMENT '项目名称',
  `project_no` varchar(255) NOT NULL COMMENT '项目编号',
  `project_type` tinyint DEFAULT NULL COMMENT '项目类型 1=环评,2=场调,3=应急预案,4=验收,5=环评入围,6=场调入围,99=其他',
  `bid_type` tinyint DEFAULT NULL COMMENT '竞价类型 1=明标,2=暗标,3=两阶段标',
  `bid_way` tinyint DEFAULT NULL COMMENT '竞价方式 1=邀请招标,2=竞争性谈判,3=竞争性磋商,4=询价采购,5=单一来源采购,6=框架协议采购,7=电子竞价,8=均价比选,9=最低价中标',
  `area` varchar(100) DEFAULT NULL COMMENT '项目所在区域',
  `project_source` varchar(100) DEFAULT NULL COMMENT '项目来源',
  `owner_company` varchar(255) DEFAULT NULL COMMENT '项目业主单位',
  `owner_contact` varchar(50) DEFAULT NULL COMMENT '业主单位联系人',
  `owner_phone` varchar(20) DEFAULT NULL COMMENT '联系方式',
  `owner_post` varchar(50) DEFAULT NULL COMMENT '职位',
  `bid_website` varchar(100) DEFAULT NULL COMMENT '选择来源哪个招标网',
  `bid_url` varchar(500) DEFAULT NULL COMMENT '招标网址',
   `price_type` tinyint DEFAULT NULL COMMENT '报价类型 1=金额 0=其他',
   `project_cycle` tinyint DEFAULT NULL COMMENT '项目周期',
  `project_amount` decimal(12,2) DEFAULT NULL COMMENT '项目金额',
  `max_price` decimal(12,2) DEFAULT NULL COMMENT '最高限价',
  `min_price` decimal(12,2) DEFAULT NULL COMMENT '最低限价',
  `price_unit` varchar(20) DEFAULT NULL COMMENT '限价单位',
  `score_method` text DEFAULT NULL COMMENT '评分方式',
  `bid_document_fee` decimal(10,2) DEFAULT NULL COMMENT '标书费',
  `platform_fee` decimal(10,2) DEFAULT NULL COMMENT '平台使用费',
  `agency_fee` decimal(10,2) DEFAULT NULL COMMENT '中标代理费',
  `bid_deposit` decimal(12,2) DEFAULT NULL COMMENT '投标保证金',
  `contract_deposit` decimal(12,2) DEFAULT NULL COMMENT '合同履约保证金',
  `question_end_time` datetime DEFAULT NULL COMMENT '质疑时间截至日期',
  `bid_time` datetime DEFAULT NULL COMMENT '投标时间',
  `qualification_require` varchar(1000) DEFAULT NULL COMMENT '资质要求',
  `document_binding_fee` decimal(10,2) DEFAULT NULL COMMENT '标书装订费',
  `remark` varchar(1000) DEFAULT NULL COMMENT '备注',
  `content_detail` text DEFAULT NULL COMMENT '项目详细内容',
  `bisness_user_id` bigint DEFAULT NULL COMMENT '业务员用户ID',
  `bisness_user_name` varchar(50) DEFAULT NULL COMMENT '业务员姓名',
  `bisness_user_phone` varchar(20) DEFAULT NULL COMMENT '业务员手机号',
   `tech_user_id` bigint DEFAULT NULL COMMENT '技术负责人用户ID',
  `tech_user_name` varchar(50) DEFAULT NULL COMMENT '技术负责人姓名',
  `tech_user_phone` varchar(20) DEFAULT NULL COMMENT '技术负责人手机号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目基本信息表';

CREATE TABLE `project_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint NOT NULL COMMENT '关联项目ID',
  `stage_type` tinyint NOT NULL COMMENT '阶段类型 1=投标阶段,2=签约阶段,3=实施阶段,4=维护阶段',
  `file_path` varchar(1000) DEFAULT NULL COMMENT '附件路径',
  `original_name` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文件名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目附件表'

CREATE TABLE `project_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint NOT NULL COMMENT '关联项目ID',
  `stage_type` tinyint NOT NULL COMMENT '阶段类型 1=投标阶段,2=签约阶段,3=实施阶段,4=维护阶段',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `stage_status` tinyint DEFAULT '0' COMMENT '阶段状态 0=未开始 1=进行中 2=已完成',
  `stage_remark` varchar(1000) DEFAULT NULL COMMENT '阶段备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目阶段表';

CREATE TABLE `project_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID（主键）',
  `project_id` bigint NOT NULL COMMENT '项目ID（关联project_info.id）',
  `stage_type` tinyint DEFAULT NULL COMMENT '项目阶段类型 1=投标阶段,2=签约阶段,3=实施阶段,4=维护阶段',
  `assign_user_id` bigint DEFAULT NULL COMMENT '任务分配的用户ID',
  `assign_user_name` varchar(50) DEFAULT NULL COMMENT '任务分配人姓名',
  `assign_user_phone` varchar(20) DEFAULT NULL COMMENT '任务分配人手机号',
  `task_type` tinyint DEFAULT NULL COMMENT '任务类型 1=业主调研,2=资料收集,3=现场勘察,4=成本估算,5=风险分析,6=方案设计,7=方案一审,8=方案二审,9=实施任务',
  `task_content` text DEFAULT NULL COMMENT '任务内容',
  `exec_user_id` bigint DEFAULT NULL COMMENT '任务执行人用户ID',
  `exec_user_name` varchar(50) DEFAULT NULL COMMENT '任务执行人姓名',
  `exec_user_phone` varchar(20) DEFAULT NULL COMMENT '任务执行人手机号',
  `need_car` tinyint DEFAULT '0' COMMENT '是否派车 0=否,1=是',
  `toll_fee` decimal(10,2) DEFAULT '0.00' COMMENT '通行费',
  `mileage` decimal(10,2) DEFAULT '0.00' COMMENT '里程数（公里）',
  `task_start_time` datetime DEFAULT NULL COMMENT '任务开始时间',
  `task_end_time` datetime DEFAULT NULL COMMENT '任务结束时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
  `deliver_type` tinyint DEFAULT NULL COMMENT '任务交付物类型 1=文档,2=表格,3=图片,4=其他',
  `task_status` tinyint DEFAULT '0' COMMENT '任务状态 0=未开始,1=执行中,2=延时执行中,3=按时完成,4=延时完成,5=未完成',
  `exec_start_time` datetime DEFAULT NULL COMMENT '任务执行开始时间',
  `exec_finish_time` datetime DEFAULT NULL COMMENT '任务完成时间',
  `task_desc` varchar(1000) DEFAULT NULL COMMENT '任务说明',
  `remark` varchar(1000) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目任务信息表';

CREATE TABLE `project_task_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `task_id` bigint NOT NULL COMMENT '任务ID（关联project_task.id）',
  `attach_type` tinyint DEFAULT NULL COMMENT '附件类型 1=分配时附件,2=执行时附件',
  `file_path` varchar(500) DEFAULT NULL COMMENT '附件路径',
  `file_name` varchar(255) DEFAULT NULL COMMENT '附件文件名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目任务执行附件表';

CREATE TABLE `sys_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID',
  `task_id` bigint DEFAULT NULL COMMENT '任务ID',
  `receive_user_id` bigint NOT NULL COMMENT '消息接收用户ID',
  `msg_type` tinyint DEFAULT '1' COMMENT '消息类型 1=任务消息,2=其他消息',
  `content` varchar(1000) NOT NULL COMMENT '消息内容',
  `read_status` tinyint DEFAULT '0' COMMENT '消息状态 0=未读,1=已读',
  `read_time` datetime DEFAULT NULL COMMENT '消息读取时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_receive_user_id` (`receive_user_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息信息表';

CREATE TABLE `project_bid_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint NOT NULL COMMENT '关联项目ID(project_info.id)',
  `bid_company_name` varchar(255) NOT NULL COMMENT '投标单位名称',
  `bid_price` varchar(500) DEFAULT NULL COMMENT '报价(字符型，支持金额、费率等)',
  `price_score` decimal(5,2) DEFAULT NULL COMMENT '报价得分',
  `business_score` decimal(5,2) DEFAULT NULL COMMENT '商务得分',
  `tech_score` decimal(5,2) DEFAULT NULL COMMENT '技术得分',
  `final_score` decimal(5,2) DEFAULT NULL COMMENT '最终得分',
  `final_rank` int DEFAULT NULL COMMENT '最终排名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投标结果表';

CREATE TABLE `project_extend` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint NOT NULL COMMENT '关联项目ID(project_info.id)',
  `is_win_bid` tinyint DEFAULT '0' COMMENT '是否中标 0=否,1=是',
  `win_bid_amount` varchar(500) DEFAULT NULL COMMENT '中标金额(字符型，支持金额、费率等)',
  `file_path` varchar(500) DEFAULT NULL COMMENT '中标通知书路径',
  `file_name` varchar(255) DEFAULT NULL COMMENT '中标通知书文件名',
  `is_sign` tinyint DEFAULT '0' COMMENT '是否签约 0=否,1=是',
  `sign_end_time` datetime DEFAULT null COMMENT '签约截止时间',
  `sign_time` datetime DEFAULT null COMMENT '签约时间',
  `is_sign` tinyint DEFAULT '0' COMMENT '是否签约 0=否,1=是',
  `is_receive_money` tinyint DEFAULT '0' COMMENT '是否收款 0=否,1=是',
  `is_deliver` tinyint DEFAULT '0' COMMENT '是否成功交付 0=否,1=是',
  `is_finish` tinyint DEFAULT '0' COMMENT '是否结束 0=否,1=是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目拓展信息表';

CREATE TABLE `sign_visit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint NOT NULL COMMENT '关联项目ID',
  `visit_target` varchar(1000) DEFAULT NULL COMMENT '拜访对象(支持多人自行填写)',
  `communicate_content` text DEFAULT NULL COMMENT '交流内容',
  `visit_start_time` datetime DEFAULT NULL COMMENT '拜访开始时间',
  `visit_end_time` datetime DEFAULT NULL COMMENT '拜访结束时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户拜访表';

CREATE TABLE `sign_visit_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `visit_id` bigint NOT NULL COMMENT '拜访记录ID(关联sign_visit.id)',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_visit_id` (`visit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拜访记录附件表';