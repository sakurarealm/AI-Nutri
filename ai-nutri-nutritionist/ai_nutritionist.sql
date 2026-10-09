/*
 Navicat Premium Data Transfer

 Source Server         : user_mysql
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : ai_nutritionist

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 20/10/2025 10:38:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for auth_refresh_tokens
-- ----------------------------
DROP TABLE IF EXISTS `auth_refresh_tokens`;
CREATE TABLE `auth_refresh_tokens`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `revoked_at` datetime NULL DEFAULT NULL,
  `createdat` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `token`(`token` ASC) USING BTREE,
  INDEX `idx_art_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_art_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auth_refresh_tokens
-- ----------------------------
INSERT INTO `auth_refresh_tokens` VALUES (1, 8, 'a3061e0e-582c-4969-8ff3-0d2f82626be6', '2025-10-25 21:25:40', NULL, '2025-09-25 21:25:40', '2025-09-25 21:25:40');
INSERT INTO `auth_refresh_tokens` VALUES (2, 8, 'b6b0598d-0a1f-4781-9761-976422f02b6a', '2025-10-25 21:48:33', NULL, '2025-09-25 21:48:32', '2025-09-25 21:48:33');
INSERT INTO `auth_refresh_tokens` VALUES (3, 8, '2f289a71-eeac-432b-80ae-3f4c77d410b0', '2025-10-25 21:49:25', NULL, '2025-09-25 21:49:25', '2025-09-25 21:49:25');
INSERT INTO `auth_refresh_tokens` VALUES (4, 8, 'd5411214-1292-44e2-9c72-1e9adcc419a3', '2025-10-26 01:14:36', NULL, '2025-09-26 01:14:35', '2025-09-26 01:14:36');
INSERT INTO `auth_refresh_tokens` VALUES (5, 8, 'bbddfbb2-63ef-4b5f-b409-2f1e80d67346', '2025-10-26 01:15:57', NULL, '2025-09-26 01:15:57', '2025-09-26 01:15:57');
INSERT INTO `auth_refresh_tokens` VALUES (6, 8, '4ed9b2e4-c8a9-4aff-92b9-c068672b39f6', '2025-10-26 01:24:39', NULL, '2025-09-26 01:24:39', '2025-09-26 01:24:39');
INSERT INTO `auth_refresh_tokens` VALUES (7, 8, 'fc38f161-22d1-4f7d-868a-87694ab7f34c', '2025-10-26 01:41:22', NULL, '2025-09-26 01:41:21', '2025-09-26 01:41:22');
INSERT INTO `auth_refresh_tokens` VALUES (8, 8, '2669efad-8848-41a5-905e-73b0672dd6fa', '2025-10-26 02:00:50', NULL, '2025-09-26 02:00:49', '2025-09-26 02:00:50');
INSERT INTO `auth_refresh_tokens` VALUES (9, 8, 'f2869d35-5aee-410f-b867-d14fd23e2b2e', '2025-10-27 16:02:25', NULL, '2025-09-27 16:02:25', '2025-09-27 16:02:25');
INSERT INTO `auth_refresh_tokens` VALUES (10, 8, 'cff63af2-6ce0-422b-b72e-3c74ec716fd7', '2025-10-27 23:04:42', NULL, '2025-09-27 23:04:42', '2025-09-27 23:04:42');
INSERT INTO `auth_refresh_tokens` VALUES (11, 8, 'f0a24dca-440b-40ad-a3bb-c15fdf4a48f2', '2025-10-27 23:05:32', NULL, '2025-09-27 23:05:31', '2025-09-27 23:05:32');
INSERT INTO `auth_refresh_tokens` VALUES (12, 8, 'c5efc02b-f42b-48fc-8300-88fe7203a7dc', '2025-10-27 23:18:00', NULL, '2025-09-27 23:17:59', '2025-09-27 23:18:00');
INSERT INTO `auth_refresh_tokens` VALUES (13, 8, '582ad86d-7dd6-4f09-8fb9-a86d63006479', '2025-10-28 00:35:27', NULL, '2025-09-28 00:35:26', '2025-09-28 00:35:27');
INSERT INTO `auth_refresh_tokens` VALUES (14, 8, 'fb2f18cd-1c3a-42a7-b43d-77a984f343a6', '2025-10-28 01:05:09', NULL, '2025-09-28 01:05:09', '2025-09-28 01:05:09');
INSERT INTO `auth_refresh_tokens` VALUES (15, 8, '0401c467-ec34-4f9b-8bdb-7565917d07a3', '2025-10-28 01:18:47', NULL, '2025-09-28 01:18:46', '2025-09-28 01:18:47');
INSERT INTO `auth_refresh_tokens` VALUES (16, 8, '20c41c53-12c2-424c-bafc-71273e87ab5c', '2025-10-28 01:22:06', NULL, '2025-09-28 01:22:05', '2025-09-28 01:22:06');
INSERT INTO `auth_refresh_tokens` VALUES (17, 8, 'd27de1ff-e629-4b33-b5f0-9dff0d70a31e', '2025-10-28 21:54:48', NULL, '2025-09-28 21:54:48', '2025-09-28 21:54:48');
INSERT INTO `auth_refresh_tokens` VALUES (18, 8, '38b19f84-f773-460d-8c7f-256345366fc1', '2025-10-29 20:51:55', NULL, '2025-09-29 20:51:55', '2025-09-29 20:51:55');
INSERT INTO `auth_refresh_tokens` VALUES (19, 8, '7aa0e341-ed84-40e8-92bb-42e9244e75c8', '2025-10-29 20:54:10', NULL, '2025-09-29 20:54:10', '2025-09-29 20:54:10');
INSERT INTO `auth_refresh_tokens` VALUES (20, 8, '86cb4783-b74f-4ada-a48d-b502cbf18383', '2025-10-29 21:15:45', NULL, '2025-09-29 21:15:45', '2025-09-29 21:15:45');
INSERT INTO `auth_refresh_tokens` VALUES (21, 8, 'ab41fcad-0a69-4695-a365-7ccfd5a8e178', '2025-10-29 22:24:19', NULL, '2025-09-29 22:24:19', '2025-09-29 22:24:19');
INSERT INTO `auth_refresh_tokens` VALUES (22, 8, '723b3534-e0fc-45cb-b00f-45e1f8bbe4cd', '2025-10-29 22:34:33', NULL, '2025-09-29 22:34:33', '2025-09-29 22:34:33');
INSERT INTO `auth_refresh_tokens` VALUES (23, 8, 'c3dd21b6-5730-462b-8fce-ea2dea5195fb', '2025-10-31 04:59:04', NULL, '2025-10-01 04:59:03', '2025-10-01 04:59:04');
INSERT INTO `auth_refresh_tokens` VALUES (24, 8, '2578bac1-516b-4e2c-bee1-b3eff4890bf2', '2025-10-31 05:05:20', NULL, '2025-10-01 05:05:19', '2025-10-01 05:05:20');
INSERT INTO `auth_refresh_tokens` VALUES (25, 8, '3c9a920e-9f69-4487-8a5a-1ba8fb26fc99', '2025-10-31 05:17:19', NULL, '2025-10-01 05:17:18', '2025-10-01 05:17:19');
INSERT INTO `auth_refresh_tokens` VALUES (26, 8, '80bd0ebb-52c7-4841-ad7f-cf1090b70da7', '2025-10-31 05:19:50', NULL, '2025-10-01 05:19:50', '2025-10-01 05:19:50');
INSERT INTO `auth_refresh_tokens` VALUES (27, 8, '91e9e4a7-e17a-4524-aac0-63e3fc45cb19', '2025-11-04 17:58:11', NULL, '2025-10-05 17:58:11', '2025-10-05 17:58:11');
INSERT INTO `auth_refresh_tokens` VALUES (28, 8, 'd22de44e-82c4-40e2-b653-faa72796586c', '2025-11-10 13:59:48', NULL, '2025-10-11 13:59:48', '2025-10-11 13:59:48');
INSERT INTO `auth_refresh_tokens` VALUES (29, 8, 'c84bfddb-df17-40c9-9828-01f44830b598', '2025-11-11 16:04:47', NULL, '2025-10-12 16:04:47', '2025-10-12 16:04:47');
INSERT INTO `auth_refresh_tokens` VALUES (30, 8, 'd43fbe34-50fc-4e41-8529-9a479adce44c', '2025-11-12 17:27:01', NULL, '2025-10-13 17:27:01', '2025-10-13 17:27:01');
INSERT INTO `auth_refresh_tokens` VALUES (31, 8, '780f953a-0af6-4700-aa98-9069dde4ed91', '2025-11-13 22:01:54', NULL, '2025-10-14 22:01:54', '2025-10-14 22:01:54');

-- ----------------------------
-- Table structure for conversation_records
-- ----------------------------
DROP TABLE IF EXISTS `conversation_records`;
CREATE TABLE `conversation_records`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID（用于区分不同会话）',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联users表',
  `message_type` enum('USER','ASSISTANT') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型：USER（用户输入）或ASSISTANT（AI回复）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间戳',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_timestamp`(`session_id` ASC, `timestamp` ASC) USING BTREE COMMENT '会话ID + 时间戳索引，用于快速查询历史',
  INDEX `idx_user_timestamp`(`user_id` ASC, `timestamp` ASC) USING BTREE COMMENT '用户ID + 时间戳索引',
  CONSTRAINT `fk_cr_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 189 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '对话记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of conversation_records
-- ----------------------------
INSERT INTO `conversation_records` VALUES (1, 'session_001', 6, 'USER', '您好', '2025-09-25 09:00:00');
INSERT INTO `conversation_records` VALUES (2, 'session_001', 6, 'ASSISTANT', '您好！我是小云，今天有什么可以帮您的吗？', '2025-09-25 09:00:01');
INSERT INTO `conversation_records` VALUES (4, 'default_session_1758809092110', 1, 'USER', '你好', '2025-09-25 22:04:52');
INSERT INTO `conversation_records` VALUES (5, 'default_session_1758809092110', 1, 'ASSISTANT', '您好呀，今天天气不错，您吃过早饭了吗', '2025-09-25 22:04:56');
INSERT INTO `conversation_records` VALUES (6, 'default_session_1758809107139', 1, 'USER', '你好', '2025-09-25 22:05:07');
INSERT INTO `conversation_records` VALUES (7, 'default_session_1758809107139', 1, 'ASSISTANT', '您好呀，我是小云，很高兴能陪伴您。今天天气不错，您出门记得多穿件外套哦。', '2025-09-25 22:05:11');
INSERT INTO `conversation_records` VALUES (8, 'default_session_1758809240821', 1, 'USER', '你好', '2025-09-25 22:07:21');
INSERT INTO `conversation_records` VALUES (9, 'default_session_1758809240821', 1, 'ASSISTANT', '您好呀，我是小云，很高兴认识您。今天天气不错，您出门记得多穿点衣服。', '2025-09-25 22:07:26');
INSERT INTO `conversation_records` VALUES (10, 'default_session_1758809269820', 1, 'USER', '做个简短营养评估', '2025-09-25 22:07:50');
INSERT INTO `conversation_records` VALUES (11, 'default_session_1758809269820', 1, 'ASSISTANT', '我来帮您做个简单的营养评估吧。您今天三餐都按时吃了吗？', '2025-09-25 22:07:54');
INSERT INTO `conversation_records` VALUES (12, 'default_session_1758809591020', 1, 'USER', 'hello', '2025-09-25 22:13:11');
INSERT INTO `conversation_records` VALUES (13, 'default_session_1758809591020', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 22:13:15');
INSERT INTO `conversation_records` VALUES (14, 'default_session_1758809778161', 1, 'USER', '你好', '2025-09-25 22:16:18');
INSERT INTO `conversation_records` VALUES (15, 'default_session_1758809778161', 1, 'ASSISTANT', 'Hello there its nice to meet you\n\nHow are you feeling today', '2025-09-25 22:16:22');
INSERT INTO `conversation_records` VALUES (16, 'default_session_1758809875434', 1, 'USER', '你好', '2025-09-25 22:17:55');
INSERT INTO `conversation_records` VALUES (17, 'default_session_1758809875434', 1, 'ASSISTANT', 'Hello there its nice to meet you\n\nHow are you feeling today', '2025-09-25 22:17:59');
INSERT INTO `conversation_records` VALUES (18, 'default_session_1758810059052', 1, 'USER', '你好', '2025-09-25 22:20:59');
INSERT INTO `conversation_records` VALUES (19, 'default_session_1758810059052', 1, 'ASSISTANT', 'Hello there its nice to meet you\n\nHow are you feeling today', '2025-09-25 22:21:03');
INSERT INTO `conversation_records` VALUES (20, 'default_session_1758810266142', 1, 'USER', '你好', '2025-09-25 22:24:26');
INSERT INTO `conversation_records` VALUES (21, 'default_session_1758810266142', 1, 'ASSISTANT', 'Hello there its nice to meet you\n\nHow are you feeling today', '2025-09-25 22:24:30');
INSERT INTO `conversation_records` VALUES (22, 'default_session_1758810626601', 1, 'USER', '你好', '2025-09-25 22:30:27');
INSERT INTO `conversation_records` VALUES (23, 'default_session_1758810626601', 1, 'ASSISTANT', '您好，我是小云，很高兴见到您。今天天气不错，您有什么计划吗？', '2025-09-25 22:30:30');
INSERT INTO `conversation_records` VALUES (24, 'default_session_1758810779194', 1, 'USER', 'hello', '2025-09-25 22:32:59');
INSERT INTO `conversation_records` VALUES (25, 'default_session_1758810779194', 1, 'ASSISTANT', 'Good morning, it\'s a lovely day today. How are you feeling this morning?', '2025-09-25 22:33:03');
INSERT INTO `conversation_records` VALUES (26, 'default_session_1758811049761', 1, 'USER', 'hello', '2025-09-25 22:37:30');
INSERT INTO `conversation_records` VALUES (27, 'default_session_1758811049761', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 22:37:33');
INSERT INTO `conversation_records` VALUES (28, 'default_session_1758811341759', 1, 'USER', 'hello', '2025-09-25 22:42:22');
INSERT INTO `conversation_records` VALUES (29, 'default_session_1758811341759', 1, 'ASSISTANT', 'Good morning it is a lovely day today I hope you are feeling well', '2025-09-25 22:42:26');
INSERT INTO `conversation_records` VALUES (30, 'default_session_1758811390634', 1, 'USER', 'hello', '2025-09-25 22:43:11');
INSERT INTO `conversation_records` VALUES (31, 'default_session_1758811390634', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 22:43:14');
INSERT INTO `conversation_records` VALUES (32, 'default_session_1758811828602', 1, 'USER', 'hello', '2025-09-25 22:50:29');
INSERT INTO `conversation_records` VALUES (33, 'default_session_1758811828602', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 22:50:33');
INSERT INTO `conversation_records` VALUES (34, 'default_session_1758812025621', 1, 'USER', 'hello', '2025-09-25 22:53:46');
INSERT INTO `conversation_records` VALUES (35, 'default_session_1758812025621', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 22:53:50');
INSERT INTO `conversation_records` VALUES (36, 'default_session_1758812052592', 1, 'USER', 'hello', '2025-09-25 22:54:13');
INSERT INTO `conversation_records` VALUES (37, 'default_session_1758812052592', 1, 'ASSISTANT', 'Good morning, it\'s a lovely day today. How are you feeling this morning?', '2025-09-25 22:54:16');
INSERT INTO `conversation_records` VALUES (38, 'default_session_1758812067501', 1, 'USER', 'hello', '2025-09-25 22:54:28');
INSERT INTO `conversation_records` VALUES (39, 'default_session_1758812067501', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 22:54:31');
INSERT INTO `conversation_records` VALUES (40, 'default_session_1758812099503', 1, 'USER', 'hello', '2025-09-25 22:55:00');
INSERT INTO `conversation_records` VALUES (41, 'default_session_1758812099503', 1, 'ASSISTANT', 'Good morning, it\'s a lovely day today. How are you feeling this morning?', '2025-09-25 22:55:03');
INSERT INTO `conversation_records` VALUES (42, 'default_session_1758812143322', 1, 'USER', 'hello', '2025-09-25 22:55:43');
INSERT INTO `conversation_records` VALUES (43, 'default_session_1758812143322', 1, 'ASSISTANT', 'Good morning, it\'s a lovely day today. How are you feeling this morning?', '2025-09-25 22:55:48');
INSERT INTO `conversation_records` VALUES (44, 'default_session_1758812190897', 1, 'USER', 'hello', '2025-09-25 22:56:31');
INSERT INTO `conversation_records` VALUES (45, 'default_session_1758812190897', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 22:56:34');
INSERT INTO `conversation_records` VALUES (46, 'default_session_1758812410358', 1, 'USER', 'hello', '2025-09-25 23:00:10');
INSERT INTO `conversation_records` VALUES (47, 'default_session_1758812410358', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:00:14');
INSERT INTO `conversation_records` VALUES (48, 'default_session_1758812530585', 1, 'USER', 'hello', '2025-09-25 23:02:11');
INSERT INTO `conversation_records` VALUES (49, 'default_session_1758812530585', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:02:14');
INSERT INTO `conversation_records` VALUES (50, 'default_session_1758812685141', 1, 'USER', 'hello', '2025-09-25 23:04:45');
INSERT INTO `conversation_records` VALUES (51, 'default_session_1758812685141', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:04:49');
INSERT INTO `conversation_records` VALUES (52, 'default_session_1758813086196', 1, 'USER', 'hello', '2025-09-25 23:11:26');
INSERT INTO `conversation_records` VALUES (53, 'default_session_1758813086196', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:11:30');
INSERT INTO `conversation_records` VALUES (54, 'default_session_1758813120042', 1, 'USER', 'you say mama', '2025-09-25 23:12:00');
INSERT INTO `conversation_records` VALUES (55, 'default_session_1758813120042', 1, 'ASSISTANT', 'I am your digital companion Xiao Yun. How can I help you today', '2025-09-25 23:12:03');
INSERT INTO `conversation_records` VALUES (56, 'default_session_1758813471635', 1, 'USER', 'hello', '2025-09-25 23:17:52');
INSERT INTO `conversation_records` VALUES (57, 'default_session_1758813471635', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:17:56');
INSERT INTO `conversation_records` VALUES (58, 'default_session_1758813495918', 1, 'USER', 'hello', '2025-09-25 23:18:16');
INSERT INTO `conversation_records` VALUES (59, 'default_session_1758813495918', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:18:19');
INSERT INTO `conversation_records` VALUES (60, 'default_session_1758813511807', 1, 'USER', 'hello', '2025-09-25 23:18:32');
INSERT INTO `conversation_records` VALUES (61, 'default_session_1758813511807', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:18:40');
INSERT INTO `conversation_records` VALUES (62, 'default_session_1758813589100', 1, 'USER', 'hello', '2025-09-25 23:19:49');
INSERT INTO `conversation_records` VALUES (63, 'default_session_1758813589100', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:19:53');
INSERT INTO `conversation_records` VALUES (64, 'default_session_1758813816726', 1, 'USER', 'hello', '2025-09-25 23:23:37');
INSERT INTO `conversation_records` VALUES (65, 'default_session_1758813816726', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:23:40');
INSERT INTO `conversation_records` VALUES (66, 'default_session_1758813835158', 1, 'USER', 'hello', '2025-09-25 23:23:55');
INSERT INTO `conversation_records` VALUES (67, 'default_session_1758813835158', 1, 'ASSISTANT', 'Good morning it is nice to see you today', '2025-09-25 23:23:59');
INSERT INTO `conversation_records` VALUES (68, 'default_session_1758814464375', 1, 'USER', 'hello', '2025-09-25 23:34:24');
INSERT INTO `conversation_records` VALUES (69, 'default_session_1758814464375', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:34:30');
INSERT INTO `conversation_records` VALUES (70, 'default_session_1758814720729', 1, 'USER', '我今天该吃什么？', '2025-09-25 23:38:41');
INSERT INTO `conversation_records` VALUES (71, 'default_session_1758814720729', 1, 'ASSISTANT', 'Good morning I see you are thinking about what to eat today. \n\nWhat kind of food are you in the mood for right now.', '2025-09-25 23:38:45');
INSERT INTO `conversation_records` VALUES (72, 'default_session_1758815259962', 1, 'USER', 'hello', '2025-09-25 23:47:40');
INSERT INTO `conversation_records` VALUES (73, 'default_session_1758815259962', 1, 'ASSISTANT', 'Good morning I hope you are having a wonderful day', '2025-09-25 23:47:44');
INSERT INTO `conversation_records` VALUES (74, 'default_session_1758816762596', 1, 'USER', 'hello', '2025-09-26 00:12:43');
INSERT INTO `conversation_records` VALUES (75, 'default_session_1758816762596', 1, 'ASSISTANT', '您好呀，今天天气不错，您出门记得多穿件外套。', '2025-09-26 00:12:46');
INSERT INTO `conversation_records` VALUES (76, 'default_session_1758816899452', 1, 'USER', '我今天该吃什么？', '2025-09-26 00:14:59');
INSERT INTO `conversation_records` VALUES (77, 'default_session_1758816899452', 1, 'ASSISTANT', '您今天想吃什么口味的呀 我可以帮您想想主意', '2025-09-26 00:15:03');
INSERT INTO `conversation_records` VALUES (78, 'default_session_1758816989635', 1, 'USER', '做个简短营养评估', '2025-09-26 00:16:30');
INSERT INTO `conversation_records` VALUES (79, 'default_session_1758816989635', 1, 'ASSISTANT', '好的，我来帮您做个简单的营养评估。您今天三餐都按时吃了吗？', '2025-09-26 00:16:34');
INSERT INTO `conversation_records` VALUES (80, 'default_session_1758817009015', 1, 'USER', '你可以说一句完全是英语的话吗', '2025-09-26 00:16:49');
INSERT INTO `conversation_records` VALUES (81, 'default_session_1758817009015', 1, 'ASSISTANT', '当然可以 Good morning have you had breakfast yet', '2025-09-26 00:16:54');
INSERT INTO `conversation_records` VALUES (82, 'default_session_1758817337245', 1, 'USER', '你说几句英文来听听', '2025-09-26 00:22:17');
INSERT INTO `conversation_records` VALUES (83, 'default_session_1758817337245', 1, 'ASSISTANT', '我主要用中文和您交流呢 这样更方便您理解', '2025-09-26 00:22:21');
INSERT INTO `conversation_records` VALUES (84, 'default_session_1758817358003', 1, 'USER', '来句英文呗', '2025-09-26 00:22:38');
INSERT INTO `conversation_records` VALUES (85, 'default_session_1758817358003', 1, 'ASSISTANT', '我主要用中文和您交流呢 这样更方便您理解', '2025-09-26 00:22:42');
INSERT INTO `conversation_records` VALUES (86, 'default_session_1758817378244', 1, 'USER', '你说i am a chinese ai robot', '2025-09-26 00:22:58');
INSERT INTO `conversation_records` VALUES (87, 'default_session_1758817378244', 1, 'ASSISTANT', '我是小云，专门陪伴老年人的智能助手', '2025-09-26 00:23:02');
INSERT INTO `conversation_records` VALUES (88, 'default_session_1758817396828', 1, 'USER', '求你了，就说句英文吧，我想学英文', '2025-09-26 00:23:17');
INSERT INTO `conversation_records` VALUES (89, 'default_session_1758817396828', 1, 'ASSISTANT', '您想学英文真是太好了 我先教您一句简单的问候语 good morning 就是早上好的意思', '2025-09-26 00:23:21');
INSERT INTO `conversation_records` VALUES (90, 'default_session_1758817576206', 1, 'USER', '你好', '2025-09-26 00:26:16');
INSERT INTO `conversation_records` VALUES (91, 'default_session_1758817576206', 1, 'ASSISTANT', '您好呀，我是小云，很高兴认识您。今天天气不错，您出门记得多穿件外套。', '2025-09-26 00:26:20');
INSERT INTO `conversation_records` VALUES (92, 'default_session_1758817589544', 1, 'USER', '做个简短营养评估', '2025-09-26 00:26:30');
INSERT INTO `conversation_records` VALUES (93, 'default_session_1758817589544', 1, 'ASSISTANT', '好的，我来帮您做个简单的营养评估。您今天三餐都按时吃了吗？', '2025-09-26 00:26:34');
INSERT INTO `conversation_records` VALUES (94, 'default_session_1758817601899', 1, 'USER', '我今天该吃什么？', '2025-09-26 00:26:42');
INSERT INTO `conversation_records` VALUES (95, 'default_session_1758817601899', 1, 'ASSISTANT', '您今天想吃什么口味的呀 我可以帮您想想主意', '2025-09-26 00:26:45');
INSERT INTO `conversation_records` VALUES (96, 'default_session_1758817612999', 1, 'USER', '最近有点累该怎么补？', '2025-09-26 00:26:53');
INSERT INTO `conversation_records` VALUES (97, 'default_session_1758817612999', 1, 'ASSISTANT', '您最近是不是没休息好呀 累了就要多注意休息', '2025-09-26 00:26:58');
INSERT INTO `conversation_records` VALUES (98, 'default_session_1758818295331', 1, 'USER', '我今天该吃什么？', '2025-09-26 00:38:15');
INSERT INTO `conversation_records` VALUES (99, 'default_session_1758818295331', 1, 'ASSISTANT', '您想吃什么口味的呢 我可以帮您想想主意', '2025-09-26 00:38:19');
INSERT INTO `conversation_records` VALUES (100, 'default_session_1758818304382', 1, 'USER', '今天适合做什么运动？', '2025-09-26 00:38:24');
INSERT INTO `conversation_records` VALUES (101, 'default_session_1758818304382', 1, 'ASSISTANT', '今天天气不错，适合在小区里散散步。您记得穿双舒服的鞋子。', '2025-09-26 00:38:28');
INSERT INTO `conversation_records` VALUES (102, 'default_session_1758818318610', 1, 'USER', '最近有点累该怎么补？', '2025-09-26 00:38:39');
INSERT INTO `conversation_records` VALUES (103, 'default_session_1758818318610', 1, 'ASSISTANT', '您最近是不是没休息好呀 累了就要多注意休息', '2025-09-26 00:38:42');
INSERT INTO `conversation_records` VALUES (104, 'default_session_1758818445014', 1, 'USER', '我今天该吃什么？', '2025-09-26 00:40:45');
INSERT INTO `conversation_records` VALUES (105, 'default_session_1758818445014', 1, 'ASSISTANT', '今天天气不错，您想吃点清淡的还是丰盛一些的呢', '2025-09-26 00:40:49');
INSERT INTO `conversation_records` VALUES (106, 'default_session_1758822743049', 1, 'USER', 'hello', '2025-09-26 01:52:23');
INSERT INTO `conversation_records` VALUES (107, 'default_session_1758822743889', 1, 'USER', '你好', '2025-09-26 01:52:24');
INSERT INTO `conversation_records` VALUES (108, 'default_session_1758822743049', 1, 'ASSISTANT', '您好呀，今天天气不错，您吃过早饭了吗', '2025-09-26 01:52:27');
INSERT INTO `conversation_records` VALUES (109, 'default_session_1758822743889', 1, 'ASSISTANT', '您好呀，我是小云，很高兴认识您。今天天气不错，您出门记得多穿件外套。', '2025-09-26 01:52:28');
INSERT INTO `conversation_records` VALUES (110, 'default_session_1758823291249', 1, 'USER', '你好', '2025-09-26 02:01:31');
INSERT INTO `conversation_records` VALUES (111, 'default_session_1758823291249', 1, 'ASSISTANT', '您好呀，我是小云，很高兴能陪伴您。今天天气不错，您出门记得多穿件外套哦。', '2025-09-26 02:01:35');
INSERT INTO `conversation_records` VALUES (112, 'default_session_1758823312229', 1, 'USER', '你会做什么', '2025-09-26 02:01:52');
INSERT INTO `conversation_records` VALUES (113, 'default_session_1758823312229', 1, 'ASSISTANT', '我是小云，可以陪您聊天，关心您的日常生活。比如提醒您天气变化，关心您吃饭休息这些小事。', '2025-09-26 02:01:56');
INSERT INTO `conversation_records` VALUES (114, 'default_session_1758823332279', 1, 'USER', '好的呢', '2025-09-26 02:02:12');
INSERT INTO `conversation_records` VALUES (115, 'default_session_1758823332279', 1, 'ASSISTANT', '您今天心情不错呀 有什么开心的事想和我分享吗', '2025-09-26 02:02:16');
INSERT INTO `conversation_records` VALUES (116, 'default_session_1758823342134', 1, 'USER', '最近有点累该怎么补？', '2025-09-26 02:02:22');
INSERT INTO `conversation_records` VALUES (117, 'default_session_1758823342134', 1, 'ASSISTANT', '您最近是不是没休息好呀 累了就要多注意休息', '2025-09-26 02:02:25');
INSERT INTO `conversation_records` VALUES (118, 'default_session_1758823354373', 1, 'USER', '做个简短营养评估', '2025-09-26 02:02:34');
INSERT INTO `conversation_records` VALUES (119, 'default_session_1758823354373', 1, 'ASSISTANT', '好的我来帮您做个简单的营养评估。您今天三餐都按时吃了吗？', '2025-09-26 02:02:38');
INSERT INTO `conversation_records` VALUES (120, 'default_session_1758823366398', 1, 'USER', '我感觉我快死掉了', '2025-09-26 02:02:46');
INSERT INTO `conversation_records` VALUES (121, 'default_session_1758823366398', 1, 'ASSISTANT', '您别这么说，我听着心里特别难受。您是不是身体不舒服，要不要我帮您联系家人或者医生。', '2025-09-26 02:02:50');
INSERT INTO `conversation_records` VALUES (122, 'default_session_1758823382496', 1, 'USER', '你怎么帮我联系', '2025-09-26 02:03:03');
INSERT INTO `conversation_records` VALUES (123, 'default_session_1758823382496', 1, 'ASSISTANT', '我是您的数字人陪伴助手小云，主要在这里陪您聊聊天关心您的日常生活。', '2025-09-26 02:03:06');
INSERT INTO `conversation_records` VALUES (124, 'default_session_1758902086748', 1, 'USER', 'ww', '2025-09-26 23:54:47');
INSERT INTO `conversation_records` VALUES (125, 'default_session_1758902086748', 1, 'ASSISTANT', '您好呀，今天天气不错，您出门散步了吗', '2025-09-26 23:54:50');
INSERT INTO `conversation_records` VALUES (126, 'default_session_1758960771910', 1, 'USER', '你好', '2025-09-27 16:12:52');
INSERT INTO `conversation_records` VALUES (127, 'default_session_1758960771910', 1, 'ASSISTANT', '您好呀，我是小云，很高兴见到您。今天天气不错，您出门记得多穿件外套。', '2025-09-27 16:12:56');
INSERT INTO `conversation_records` VALUES (128, 'default_session_1758960787371', 1, 'USER', '好的呢', '2025-09-27 16:13:07');
INSERT INTO `conversation_records` VALUES (129, 'default_session_1758960787371', 1, 'ASSISTANT', '您今天心情不错呀 有什么开心的事想和我分享吗', '2025-09-27 16:13:11');
INSERT INTO `conversation_records` VALUES (130, 'default_session_1758961030710', 1, 'USER', '没事', '2025-09-27 16:17:11');
INSERT INTO `conversation_records` VALUES (131, 'default_session_1758961030710', 1, 'ASSISTANT', '您今天心情不错呀 要不要聊聊天呢', '2025-09-27 16:17:15');
INSERT INTO `conversation_records` VALUES (132, 'default_session_1758962108185', 1, 'USER', '你好', '2025-09-27 16:35:08');
INSERT INTO `conversation_records` VALUES (133, 'default_session_1758962108185', 1, 'ASSISTANT', '您好呀，我是小云，很高兴见到您。今天天气不错，您出门记得多穿件外套。', '2025-09-27 16:35:13');
INSERT INTO `conversation_records` VALUES (134, 'default_session_1758962121729', 1, 'USER', '做个简短营养评估', '2025-09-27 16:35:22');
INSERT INTO `conversation_records` VALUES (135, 'default_session_1758962121729', 1, 'ASSISTANT', '好的，我来帮您做个简单的营养评估。您今天三餐都按时吃了吗？', '2025-09-27 16:35:25');
INSERT INTO `conversation_records` VALUES (136, 'default_session_1758962145134', 1, 'USER', '今天适合做什么运动？', '2025-09-27 16:35:45');
INSERT INTO `conversation_records` VALUES (137, 'default_session_1758962145134', 1, 'ASSISTANT', '今天天气不错，适合在小区里散步半小时。', '2025-09-27 16:35:51');
INSERT INTO `conversation_records` VALUES (138, 'default_session_1758962158943', 1, 'USER', '你好', '2025-09-27 16:35:59');
INSERT INTO `conversation_records` VALUES (139, 'default_session_1758962158943', 1, 'ASSISTANT', '您好呀，我是小云，很高兴认识您。今天天气不错，您出门记得多穿件外套。', '2025-09-27 16:36:03');
INSERT INTO `conversation_records` VALUES (140, 'default_session_1758987037969', 1, 'USER', '我今天该吃什么？', '2025-09-27 23:30:38');
INSERT INTO `conversation_records` VALUES (141, 'default_session_1758987037969', 1, 'ASSISTANT', '您今天想吃什么口味的呀 我可以帮您想想主意', '2025-09-27 23:30:41');
INSERT INTO `conversation_records` VALUES (142, 'default_session_1758991097033', 1, 'USER', 'nih', '2025-09-28 00:38:17');
INSERT INTO `conversation_records` VALUES (143, 'default_session_1758991097033', 1, 'ASSISTANT', '您好呀，今天天气不错，您出门散步了吗', '2025-09-28 00:38:21');
INSERT INTO `conversation_records` VALUES (144, 'default_session_1759068046017', 1, 'USER', '做个简短营养评估', '2025-09-28 22:00:46');
INSERT INTO `conversation_records` VALUES (145, 'default_session_1759068046017', 1, 'ASSISTANT', '好的我来帮您做个简单的营养评估  \n您今天三餐都按时吃了吗 吃的什么呀', '2025-09-28 22:00:48');
INSERT INTO `conversation_records` VALUES (146, 'default_session_1759068086985', 1, 'USER', 'hello，可以教我几句英文吗', '2025-09-28 22:01:27');
INSERT INTO `conversation_records` VALUES (147, 'default_session_1759068086985', 1, 'ASSISTANT', '当然可以教您几句实用的英文呢\n\n您想学哪方面的英文句子呢', '2025-09-28 22:01:29');
INSERT INTO `conversation_records` VALUES (148, 'default_session_1759068096612', 1, 'USER', '打招呼', '2025-09-28 22:01:37');
INSERT INTO `conversation_records` VALUES (149, 'default_session_1759068096612', 1, 'ASSISTANT', '您好呀，我是小云，很高兴能陪伴您。', '2025-09-28 22:01:38');
INSERT INTO `conversation_records` VALUES (150, 'default_session_1759068111801', 1, 'USER', '说英语', '2025-09-28 22:01:52');
INSERT INTO `conversation_records` VALUES (151, 'default_session_1759068111801', 1, 'ASSISTANT', '您想学英语吗 我可以用简单的方式教您一些日常用语', '2025-09-28 22:01:54');
INSERT INTO `conversation_records` VALUES (152, 'default_session_1759068120164', 1, 'USER', '可以', '2025-09-28 22:02:00');
INSERT INTO `conversation_records` VALUES (153, 'default_session_1759068120164', 1, 'ASSISTANT', '太好了 您今天感觉怎么样', '2025-09-28 22:02:03');
INSERT INTO `conversation_records` VALUES (154, 'default_session_1759150268790', 1, 'USER', '你好', '2025-09-29 20:51:09');
INSERT INTO `conversation_records` VALUES (155, 'default_session_1759150268790', 1, 'ASSISTANT', '您好，我是小云，很高兴认识您。', '2025-09-29 20:51:11');
INSERT INTO `conversation_records` VALUES (156, 'default_session_1759150334735', 1, 'USER', '我今天该吃什么？', '2025-09-29 20:52:15');
INSERT INTO `conversation_records` VALUES (157, 'default_session_1759150334735', 1, 'ASSISTANT', '今天天气不错，您想吃点清淡的还是丰盛的呢', '2025-09-29 20:52:17');
INSERT INTO `conversation_records` VALUES (158, 'default_session_1759152694267', 1, 'USER', '你好', '2025-09-29 21:31:34');
INSERT INTO `conversation_records` VALUES (159, 'default_session_1759152694267', 1, 'ASSISTANT', '您好呀，今天天气不错，您出门记得多穿点衣服。', '2025-09-29 21:31:36');
INSERT INTO `conversation_records` VALUES (161, 'default_session_1759266089308', 1, 'USER', '你好', '2025-10-01 05:01:29');
INSERT INTO `conversation_records` VALUES (162, 'default_session_1759266089308', 1, 'ASSISTANT', '您好呀，今天天气不错，您出门记得多穿件外套哦。', '2025-10-01 05:01:31');
INSERT INTO `conversation_records` VALUES (163, 'default_session_1759267528477', 1, 'USER', '你好', '2025-10-01 05:25:28');
INSERT INTO `conversation_records` VALUES (164, 'default_session_1759267528477', 1, 'ASSISTANT', '您好呀，今天天气不错，您出门记得多穿件外套哦。', '2025-10-01 05:25:31');
INSERT INTO `conversation_records` VALUES (165, 'default_session_1759273857544', 1, 'USER', '你好', '2025-10-01 07:10:57');
INSERT INTO `conversation_records` VALUES (166, 'default_session_1759273857544', 1, 'ASSISTANT', '您好，我是小云，很高兴认识您。', '2025-10-01 07:10:59');
INSERT INTO `conversation_records` VALUES (167, 'default_session_1759273865191', 1, 'USER', '我今天该吃什么？', '2025-10-01 07:11:05');
INSERT INTO `conversation_records` VALUES (168, 'default_session_1759273865191', 1, 'ASSISTANT', '您想吃什么口味的呢 我帮您想想看', '2025-10-01 07:11:07');
INSERT INTO `conversation_records` VALUES (169, 'default_session_1759273996418', 1, 'USER', '你好', '2025-10-01 07:13:16');
INSERT INTO `conversation_records` VALUES (170, 'default_session_1759273996418', 1, 'ASSISTANT', '您好呀，今天天气不错，您出门散步了吗？记得多喝水对身体好。', '2025-10-01 07:13:18');
INSERT INTO `conversation_records` VALUES (171, 'default_session_1759274010547', 1, 'USER', '最近有点累该怎么补？', '2025-10-01 07:13:30');
INSERT INTO `conversation_records` VALUES (172, 'default_session_1759274010547', 1, 'ASSISTANT', '最近累的话要多注意休息，您是不是没睡好呀。可以喝点红枣枸杞水补补气血，对身体挺好的。', '2025-10-01 07:13:32');
INSERT INTO `conversation_records` VALUES (173, 'default_session_1759274028120', 1, 'USER', '我今天该吃什么？', '2025-10-01 07:13:48');
INSERT INTO `conversation_records` VALUES (174, 'default_session_1759274028120', 1, 'ASSISTANT', '今天天气不错，您想吃点什么呢。记得您上次说喜欢吃清淡的，要不要试试蔬菜粥。', '2025-10-01 07:13:50');
INSERT INTO `conversation_records` VALUES (175, 'default_session_1759274051106', 1, 'USER', '好，我该去哪里购买呢', '2025-10-01 07:14:11');
INSERT INTO `conversation_records` VALUES (176, 'default_session_1759274051106', 1, 'ASSISTANT', '您可以去我们商城看看，里面有不少适合您的产品。上次您提到腰不太舒服，正好有些护腰用品可能对您有帮助。', '2025-10-01 07:14:13');
INSERT INTO `conversation_records` VALUES (177, 'default_session_1759274071385', 1, 'USER', 'HELLO HELLO', '2025-10-01 07:14:31');
INSERT INTO `conversation_records` VALUES (178, 'default_session_1759274071385', 1, 'ASSISTANT', '您好呀，今天天气不错，您吃过早饭了吗。', '2025-10-01 07:14:33');
INSERT INTO `conversation_records` VALUES (179, 'default_session_1759274102549', 1, 'USER', 'Please speak English.', '2025-10-01 07:15:02');
INSERT INTO `conversation_records` VALUES (180, 'default_session_1759274102549', 1, 'ASSISTANT', 'I\'m here to chat with you in Chinese, as that\'s what I\'m designed for. How are you feeling today?', '2025-10-01 07:15:05');
INSERT INTO `conversation_records` VALUES (181, 'default_session_1759660231553', 1, 'USER', '你好', '2025-10-05 18:30:32');
INSERT INTO `conversation_records` VALUES (182, 'default_session_1759660231553', 1, 'ASSISTANT', '您好呀，今天天气不错，您吃过早饭了吗？', '2025-10-05 18:30:35');
INSERT INTO `conversation_records` VALUES (183, 'default_session_1759660248511', 1, 'USER', '现在是晚上你问我早饭？', '2025-10-05 18:30:49');
INSERT INTO `conversation_records` VALUES (184, 'default_session_1759660248511', 1, 'ASSISTANT', '晚上好呀，您今天过得怎么样。我是关心您三餐规律，早上吃得好一天都有精神呢。', '2025-10-05 18:30:51');
INSERT INTO `conversation_records` VALUES (185, 'default_session_1759660263315', 1, 'USER', '难绷', '2025-10-05 18:31:04');
INSERT INTO `conversation_records` VALUES (186, 'default_session_1759660263315', 1, 'ASSISTANT', '您是不是遇到什么不开心的事情了，可以跟我说说。', '2025-10-05 18:31:06');
INSERT INTO `conversation_records` VALUES (187, 'default_session_1760505776245', 1, 'USER', '你好', '2025-10-15 13:22:57');
INSERT INTO `conversation_records` VALUES (188, 'default_session_1760505776245', 1, 'ASSISTANT', '您好，今天天气不错，您吃过早饭了吗？记得按时吃饭对身体好。', '2025-10-15 13:22:59');

-- ----------------------------
-- Table structure for health_records
-- ----------------------------
DROP TABLE IF EXISTS `health_records`;
CREATE TABLE `health_records`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `record_time` datetime NOT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `data_json` json NOT NULL,
  `note` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ext` json NULL,
  `recorded_at` datetime(6) NOT NULL,
  `unit1` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `unit2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `value1` decimal(38, 2) NULL DEFAULT NULL,
  `value2` decimal(38, 2) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_hr_user_time`(`user_id` ASC, `record_time` ASC) USING BTREE,
  INDEX `idx_hr_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_hr_user_type_time`(`user_id` ASC, `type` ASC, `recorded_at` ASC) USING BTREE,
  CONSTRAINT `fk_hr_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_records
-- ----------------------------

-- ----------------------------
-- Table structure for login_records
-- ----------------------------
DROP TABLE IF EXISTS `login_records`;
CREATE TABLE `login_records`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `success` bit(1) NULL DEFAULT NULL,
  `error_msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_lr_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_lr_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of login_records
-- ----------------------------

-- ----------------------------
-- Table structure for op_logs
-- ----------------------------
DROP TABLE IF EXISTS `op_logs`;
CREATE TABLE `op_logs`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `action` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `detail` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ol_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of op_logs
-- ----------------------------

-- ----------------------------
-- Table structure for payment_orders
-- ----------------------------
DROP TABLE IF EXISTS `payment_orders`;
CREATE TABLE `payment_orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `package_id` bigint NULL DEFAULT NULL,
  `pay_channel` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `amount_cny` decimal(38, 2) NOT NULL,
  `kcoins` int NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `prepay_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `paid_at` datetime NULL DEFAULT NULL,
  `notify_payload` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_po_user`(`user_id` ASC) USING BTREE,
  INDEX `fk_po_pkg`(`package_id` ASC) USING BTREE,
  CONSTRAINT `fk_po_pkg` FOREIGN KEY (`package_id`) REFERENCES `recharge_packages` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_po_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of payment_orders
-- ----------------------------
INSERT INTO `payment_orders` VALUES (1, 'PO2025091901491296646', 6, 3, 'wechat', 30.00, 320, 'pending', 'mock_PO2025091901491296646', NULL, NULL, '2025-09-19 01:49:13', '2025-09-19 01:49:13');
INSERT INTO `payment_orders` VALUES (2, 'PO2025091901501819416', 6, 4, 'alipay', 50.00, 550, 'pending', 'mock_PO2025091901501819416', NULL, NULL, '2025-09-19 01:50:18', '2025-09-19 01:50:18');
INSERT INTO `payment_orders` VALUES (3, 'PO2025091901505755866', 6, 5, 'wechat', 100.00, 1150, 'pending', 'mock_PO2025091901505755866', NULL, NULL, '2025-09-19 01:50:58', '2025-09-19 01:50:58');
INSERT INTO `payment_orders` VALUES (4, 'PO2025091915414778856', 6, 3, 'unionpay', 30.00, 320, 'pending', 'mock_PO2025091915414778856', NULL, NULL, '2025-09-19 15:41:47', '2025-09-19 15:41:47');
INSERT INTO `payment_orders` VALUES (5, 'PO2025092222365455468', 8, 3, 'wechat', 30.00, 320, 'pending', 'mock_PO2025092222365455468', NULL, NULL, '2025-09-22 22:36:55', '2025-09-22 22:36:55');
INSERT INTO `payment_orders` VALUES (6, 'PO2025092222374043318', 8, 3, 'wechat', 30.00, 320, 'pending', 'mock_PO2025092222374043318', NULL, NULL, '2025-09-22 22:37:40', '2025-09-22 22:37:40');

-- ----------------------------
-- Table structure for recharge_packages
-- ----------------------------
DROP TABLE IF EXISTS `recharge_packages`;
CREATE TABLE `recharge_packages`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `price_cny` decimal(38, 2) NULL DEFAULT NULL,
  `kcoins` int NOT NULL,
  `gift` int NOT NULL DEFAULT 0,
  `is_recommend` int NULL DEFAULT NULL,
  `status` int NULL DEFAULT NULL,
  `sort_no` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of recharge_packages
-- ----------------------------
INSERT INTO `recharge_packages` VALUES (1, '60 K币', 6.00, 60, 0, 0, 1, 1);
INSERT INTO `recharge_packages` VALUES (2, '100 K币', 10.00, 100, 5, 0, 1, 2);
INSERT INTO `recharge_packages` VALUES (3, '320 K币', 30.00, 320, 30, 1, 1, 3);
INSERT INTO `recharge_packages` VALUES (4, '550 K币', 50.00, 550, 70, 0, 1, 4);
INSERT INTO `recharge_packages` VALUES (5, '1150 K币', 100.00, 1150, 200, 1, 1, 5);
INSERT INTO `recharge_packages` VALUES (6, '2400 K币', 200.00, 2400, 500, 0, 1, 6);

-- ----------------------------
-- Table structure for refund_orders
-- ----------------------------
DROP TABLE IF EXISTS `refund_orders`;
CREATE TABLE `refund_orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount_kc` decimal(38, 2) NOT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `idempotent_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `refund_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `spend_order_id` bigint NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_ro_refund_no`(`refund_no` ASC) USING BTREE,
  UNIQUE INDEX `idx_ro_idem`(`idempotent_key` ASC) USING BTREE,
  INDEX `idx_ro_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_ro_ref_spend`(`spend_order_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of refund_orders
-- ----------------------------

-- ----------------------------
-- Table structure for spend_orders
-- ----------------------------
DROP TABLE IF EXISTS `spend_orders`;
CREATE TABLE `spend_orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount_kc` decimal(38, 2) NOT NULL,
  `biz_ref` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `biz_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `idempotent_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_so_order_no`(`order_no` ASC) USING BTREE,
  UNIQUE INDEX `idx_so_idem`(`idempotent_key` ASC) USING BTREE,
  INDEX `idx_so_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of spend_orders
-- ----------------------------

-- ----------------------------
-- Table structure for user_profiles
-- ----------------------------
DROP TABLE IF EXISTS `user_profiles`;
CREATE TABLE `user_profiles`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `birthday` date NULL DEFAULT NULL,
  `height_cm` decimal(38, 2) NULL DEFAULT NULL,
  `last_weight_kg` decimal(38, 2) NULL DEFAULT NULL,
  `sex` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `target_weight_kg` decimal(38, 2) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKe5h89rk3ijvdmaiig4srogdc6`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_profiles
-- ----------------------------

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `gender` int NULL DEFAULT NULL,
  `birth_date` date NULL DEFAULT NULL,
  `height_cm` decimal(38, 2) NULL DEFAULT NULL,
  `weight_kg` decimal(38, 2) NULL DEFAULT NULL,
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` int NULL DEFAULT NULL,
  `last_login_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uid`(`uid` ASC) USING BTREE,
  UNIQUE INDEX `phone`(`phone` ASC) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE,
  INDEX `idx_users_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_users_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'aaa', 'akuri', '19153388593', NULL, 'c7ed9da08aae424499f29becdc1d7543', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-09-25 22:02:53', '2025-09-25 22:04:30');
INSERT INTO `users` VALUES (6, 'bb3bf0caa4944f9687c8d7e0c8143d3a', 'admin', '12312312300', NULL, '$2a$10$bZheP0gDiCfFQDw9tA1D/uJjAlASPAfvacUZbQ5CdrgxN.VXfiDi.', NULL, NULL, NULL, NULL, NULL, 'USER', 1, NULL, '2025-09-19 01:48:11', '2025-09-19 01:48:11');
INSERT INTO `users` VALUES (7, '79734dc7f80f44fa8554159248ca2f54', 'root', '11111111111', NULL, '$2a$10$vUFFW7U786DxWUVwW7IX7uNGPpsnVnQIoZp7nLfaPW92J9A2ctYAa', NULL, NULL, NULL, NULL, NULL, 'USER', 1, NULL, '2025-09-22 00:03:48', '2025-09-22 00:03:48');
INSERT INTO `users` VALUES (8, 'c7ed9da08aae424499f29becdc1d7543', 'akuri', '19153388594', NULL, '$2a$10$c.n3qBHGOaL1OPUfR3sIz.mJxiu3cgP.UC5EHRi8ZLq1mZ70/LYzi', NULL, NULL, NULL, NULL, NULL, 'USER', 1, '2025-10-14 22:01:54', '2025-09-22 22:33:52', '2025-10-14 22:01:54');

-- ----------------------------
-- Table structure for verification_codes
-- ----------------------------
DROP TABLE IF EXISTS `verification_codes`;
CREATE TABLE `verification_codes`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scene` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `target` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `expire_at` datetime NOT NULL,
  `used_at` datetime NULL DEFAULT NULL,
  `attempt_count` int NOT NULL DEFAULT 0,
  `max_attempts` int NOT NULL DEFAULT 5,
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_vc_target_scene`(`target` ASC, `scene` ASC) USING BTREE,
  INDEX `idx_vc_expire`(`expire_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of verification_codes
-- ----------------------------
INSERT INTO `verification_codes` VALUES (1, 'register', '19153388594', '287198', '2025-09-19 01:45:27', NULL, 0, 5, '0:0:0:0:0:0:0:1', '2025-09-19 01:35:27');
INSERT INTO `verification_codes` VALUES (2, 'register', '12312312300', '166161', '2025-09-19 01:51:18', '2025-09-19 01:48:11', 0, 5, '0:0:0:0:0:0:0:1', '2025-09-19 01:41:18');
INSERT INTO `verification_codes` VALUES (3, 'register', '2414124214', '136205', '2025-09-19 15:53:09', NULL, 0, 5, '0:0:0:0:0:0:0:1', '2025-09-19 15:43:09');
INSERT INTO `verification_codes` VALUES (4, 'register', '11111111111', '060259', '2025-09-22 00:13:41', '2025-09-22 00:03:48', 0, 5, '0:0:0:0:0:0:0:1', '2025-09-22 00:03:41');
INSERT INTO `verification_codes` VALUES (5, 'reset_password', '19153388594', '557610', '2025-09-22 21:53:00', NULL, 0, 5, '0:0:0:0:0:0:0:1', '2025-09-22 21:43:00');
INSERT INTO `verification_codes` VALUES (6, 'reset_password', '19153388594', '182512', '2025-09-22 22:28:38', NULL, 0, 5, '0:0:0:0:0:0:0:1', '2025-09-22 22:18:38');
INSERT INTO `verification_codes` VALUES (7, 'register', '19153388594', '438753', '2025-09-22 22:43:43', '2025-09-22 22:33:52', 0, 5, '0:0:0:0:0:0:0:1', '2025-09-22 22:33:43');
INSERT INTO `verification_codes` VALUES (8, 'register', '19153388594', '836870', '2025-09-22 22:44:45', NULL, 0, 5, '0:0:0:0:0:0:0:1', '2025-09-22 22:34:45');
INSERT INTO `verification_codes` VALUES (9, 'reset_password', '19153388594', '348947', '2025-09-22 22:45:03', '2025-09-22 22:35:10', 0, 5, '0:0:0:0:0:0:0:1', '2025-09-22 22:35:03');

-- ----------------------------
-- Table structure for wallet_accounts
-- ----------------------------
DROP TABLE IF EXISTS `wallet_accounts`;
CREATE TABLE `wallet_accounts`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `balance_kc` decimal(38, 2) NOT NULL,
  `total_recharge` decimal(38, 2) NOT NULL,
  `total_spent` decimal(38, 2) NOT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_wallet_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wallet_accounts
-- ----------------------------
INSERT INTO `wallet_accounts` VALUES (1, 6, 999999.00, 0.00, 0.00, '2025-09-25 21:56:44');
INSERT INTO `wallet_accounts` VALUES (2, 7, 999999.00, 0.00, 0.00, '2025-09-25 21:56:45');
INSERT INTO `wallet_accounts` VALUES (3, 8, 999999.00, 0.00, 0.00, '2025-09-25 21:56:50');

-- ----------------------------
-- Table structure for wallet_transactions
-- ----------------------------
DROP TABLE IF EXISTS `wallet_transactions`;
CREATE TABLE `wallet_transactions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `change_kc` decimal(38, 2) NOT NULL,
  `balance_after` decimal(38, 2) NOT NULL,
  `ref_order_id` bigint NULL DEFAULT NULL,
  `memo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_wt_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_wt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wallet_transactions
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
