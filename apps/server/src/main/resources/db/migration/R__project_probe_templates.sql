INSERT INTO project_probe_template
(template_code, dimension, applicable_tags_json, objective, required_evidence_json, scoring_rubric_json, follow_up_rules_json, enabled, version)
VALUES
('project-authenticity-v1', 'AUTHENTICITY', JSON_ARRAY('*'),
 '验证候选人是否能够用可核验的过程细节说明项目确实参与过，而不是复述简历结论。',
 JSON_ARRAY('具体业务背景与约束', '亲历的实施步骤或决策过程', '可复述的输入输出、现象或操作细节', '前后回答保持一致'),
 JSON_OBJECT('high', '能够给出连贯、具体且前后一致的亲历证据', 'medium', '能说明主要过程但细节或证据不足', 'low', '主要复述概念或结果，无法说明实际过程'),
 JSON_OBJECT('maxFollowUps', 3, 'focus', JSON_ARRAY('过程细节', '时间顺序', '可核验现象'), 'avoid', JSON_ARRAY('诱导标准答案', '直接判定造假')),
 1, 1),

('project-ownership-v1', 'OWNERSHIP', JSON_ARRAY('*'),
 '澄清候选人的个人职责、实际贡献、协作对象以及本人没有负责的边界。',
 JSON_ARRAY('本人负责的具体任务', '与团队成员或上下游的分工', '本人做出的关键决策或交付物', '能够区分团队成果与个人贡献'),
 JSON_OBJECT('high', '职责边界清楚并能给出个人决策和交付证据', 'medium', '能说明主要职责但团队与个人贡献仍有混淆', 'low', '只使用“我们”描述，无法说明个人工作'),
 JSON_OBJECT('maxFollowUps', 3, 'focus', JSON_ARRAY('个人动作', '协作边界', '决策责任'), 'avoid', JSON_ARRAY('将团队成果直接归为个人成果')),
 1, 1),

('project-metric-v1', 'METRIC', JSON_ARRAY('performance', 'availability', 'business-result'),
 '验证项目指标的基线、统计口径、测量方式和改动后的可归因结果。',
 JSON_ARRAY('优化前基线', '优化后结果', '采样周期与统计口径', '测量工具或数据来源', '排除其他变量的依据'),
 JSON_OBJECT('high', '指标、基线、口径和测量方法完整且可解释', 'medium', '有明确结果但基线或测量方法不完整', 'low', '只有百分比或结论，没有可信测量过程'),
 JSON_OBJECT('maxFollowUps', 3, 'focus', JSON_ARRAY('基线', '口径', '测量方法', '归因'), 'avoid', JSON_ARRAY('替候选人编造指标')),
 1, 1),

('project-principle-v1', 'PRINCIPLE', JSON_ARRAY('java', 'spring', 'mysql', 'redis', 'mq', 'distributed-system'),
 '验证候选人是否理解项目中关键技术的底层机制，并能把原理与实际行为联系起来。',
 JSON_ARRAY('关键机制或执行流程', '该机制在项目中的具体作用', '适用边界与常见误区', '能够解释观测到的现象'),
 JSON_OBJECT('high', '原理准确并能解释项目现象和适用边界', 'medium', '概念基本正确但与项目联系或边界不足', 'low', '只会背术语，无法解释系统行为'),
 JSON_OBJECT('maxFollowUps', 3, 'focus', JSON_ARRAY('执行流程', '项目映射', '适用边界'), 'avoid', JSON_ARRAY('长时间停留在无关八股知识点')),
 1, 1),

('project-tradeoff-v1', 'TRADEOFF', JSON_ARRAY('architecture', 'technology-choice', 'engineering'),
 '验证候选人是否理解方案选择依据、被放弃的替代方案以及选择带来的成本。',
 JSON_ARRAY('业务和工程约束', '至少一个可行替代方案', '最终选择的收益与代价', '方案失效或需要重构的条件'),
 JSON_OBJECT('high', '能基于约束比较方案并主动说明代价和演进条件', 'medium', '能解释选择理由但替代方案或代价不足', 'low', '以“大家都这样用”或单一优点代替取舍'),
 JSON_OBJECT('maxFollowUps', 3, 'focus', JSON_ARRAY('约束', '替代方案', '代价', '演进条件'), 'avoid', JSON_ARRAY('只询问技术名词优缺点')),
 1, 1),

('project-incident-v1', 'INCIDENT', JSON_ARRAY('availability', 'operations', 'troubleshooting'),
 '验证候选人面对故障时的发现、止损、定位、恢复和复盘能力。',
 JSON_ARRAY('故障现象与影响范围', '告警或发现方式', '排查假设与证据链', '止损和恢复措施', '根因与后续改进'),
 JSON_OBJECT('high', '能够按时间顺序给出证据驱动的处置闭环', 'medium', '能说明主要排查和修复但证据链或复盘不足', 'low', '只有最终原因，无法说明定位和恢复过程'),
 JSON_OBJECT('maxFollowUps', 3, 'focus', JSON_ARRAY('影响', '证据链', '止损', '根因', '复盘'), 'avoid', JSON_ARRAY('假设候选人一定经历过线上事故')),
 1, 1),

('project-scale-v1', 'SCALE', JSON_ARRAY('performance', 'distributed-system', 'capacity'),
 '验证候选人能否在业务量或数据量变化后识别瓶颈并提出有依据的演进顺序。',
 JSON_ARRAY('当前容量与关键假设', '最先出现的瓶颈及判断依据', '监控和容量验证方式', '分阶段扩展方案', '新增复杂度与成本'),
 JSON_OBJECT('high', '能基于容量假设定位瓶颈并给出分阶段演进方案', 'medium', '能提出扩展手段但缺少瓶颈依据或优先级', 'low', '直接堆叠中间件，无法说明何时需要'),
 JSON_OBJECT('maxFollowUps', 3, 'focus', JSON_ARRAY('容量假设', '瓶颈证据', '演进顺序', '成本'), 'avoid', JSON_ARRAY('无约束地要求微服务或分布式方案')),
 1, 1)
ON DUPLICATE KEY UPDATE
    dimension = VALUES(dimension),
    applicable_tags_json = VALUES(applicable_tags_json),
    objective = VALUES(objective),
    required_evidence_json = VALUES(required_evidence_json),
    scoring_rubric_json = VALUES(scoring_rubric_json),
    follow_up_rules_json = VALUES(follow_up_rules_json),
    enabled = VALUES(enabled),
    version = VALUES(version);
